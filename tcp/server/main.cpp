#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <vector>
#include <thread>
#include <atomic>
#include <mutex>
#include <algorithm>
#include <signal.h>
#include <map>
#include <condition_variable>
#include <set>
#include <poll.h>
#include <iostream>

class Client;

std::atomic_bool is_running;
std::atomic_int accept_file_descriptor;
std::mutex clients_mutex;
std::condition_variable_any clients_condition;
std::vector<Client> clients;
std::mutex clients_channel_mutex;
std::condition_variable_any clients_channel_condition;
std::string clients_channel;

class Client {
public:
    int descriptor = -1;
    bool is_nickname_set = false;
    std::string nickname = "";
    std::mutex channel_mutex;
    std::string in_channel = "";

    Client(int socket) {
        descriptor = socket;
    }

    Client(const Client &other) {
        descriptor = other.descriptor;
        is_nickname_set = other.is_nickname_set;
        nickname = other.nickname;
        in_channel = other.in_channel;
    }

    void recieve_message(std::string message) {
        channel_mutex.lock();
        in_channel += message;
        clients_condition.notify_all();
        channel_mutex.unlock();
    }

    void process() {
        // we can create new thread to process in_channel update, but we do it inplace.
        while (true) {
            channel_mutex.lock();
            if (in_channel.size() >= 4) {
                uint32_t length = ntohl(*(uint32_t *) in_channel.data());
                if (in_channel.size() >= length + 4) {
                    if (!is_nickname_set) {
                        is_nickname_set = true;
                        nickname = in_channel.substr(4, length);
                    } else {
                        time_t rawtime;
                        struct tm *timeinfo;
                        char buffer[80];

                        time(&rawtime);
                        timeinfo = localtime(&rawtime);

                        strftime(buffer, 80, "%H:%M", timeinfo);
                        std::string broadcast_message =
                                "<" + std::string(buffer) + "> " + " [" + nickname + "]: " +
                                in_channel.substr(4, length) + "\n";

                        clients_channel_mutex.lock();
                        clients_channel += broadcast_message;
                        clients_channel_condition.notify_all();
                        clients_channel_mutex.unlock();
                    }
                    in_channel.erase(0, length + 4);
                    channel_mutex.unlock();
                } else {
                    channel_mutex.unlock();
                    return;
                }
            } else {
                channel_mutex.unlock();
                return;
            }
        }
    }
};

void sigint_handler_callback(int signum) {
    is_running = false;
}

int writen(int fd, const char *buf, int len) {
    int count = 0;
    while (len != 0) {
        int n = write(fd, buf, len);
        if (n < 0) {
            perror("ERROR on writing");
            return n;
        }
        len -= n;
        buf += n;
        count += n;
    }
    return count;
}

void process_loop() {
    while (is_running) {
        clients_mutex.lock();
        for (auto &client : clients) {
            client.process();
        }
        clients_condition.wait(clients_mutex);
        clients_mutex.unlock();
    }
}

void write_loop() {
    while (is_running) {
        clients_mutex.lock();
        clients_channel_mutex.lock();
        if (!clients_channel.empty()) {
            for (auto &client : clients) {
                writen(client.descriptor, clients_channel.data(), clients_channel.size());
            }
            clients_mutex.unlock();
            clients_channel.clear();
        }
        clients_mutex.unlock();
        clients_channel_condition.wait(clients_channel_mutex);
        clients_channel_mutex.unlock();
    }
}

void poll_loop() {
    while (is_running) {
        clients_mutex.lock();
        int clients_count = clients.size();
        pollfd fds[clients.size()];
        int cur = 0;
        for (auto &client : clients) {
            fds[cur].fd = client.descriptor;
            fds[cur].events = POLLIN;
            cur++;
        }
        clients_mutex.unlock();
        int ready = poll(fds, clients_count, 1000); // every second we should refresh clients
        if (ready == -1) {
            perror("poll error");
            is_running = false;
            exit(1);
        }
        if (ready != 0) {
            for (int i = 0; i < clients_count; ++i) {
                if (fds[i].revents & POLLIN) {
                    char buffer[256];
                    bzero(buffer, 256);
                    int n = read(fds[i].fd, buffer, 255);
                    if(n < 0) {
                        perror("read error");
                        exit(1);
                    }
                    if(n == 0) {
                        perror("TODO read 0");
                        exit(1);
                    }
                    printf("Read %d bytes from %d\n", n, fds[i].fd);
                    fflush(stdout);
                    std::string message(buffer, n);
                    clients_mutex.lock();
                    for (auto &client : clients) {
                        if (client.descriptor == fds[i].fd) {
                            client.recieve_message(message);
                            break;
                        }
                    }
                    clients_mutex.unlock();
                }
            }
        }
    }
}

int main(int argc, char *argv[]) {
    struct sigaction sigint_handler{};
    sigint_handler.sa_handler = sigint_handler_callback;
    sigemptyset(&sigint_handler.sa_mask);
    sigaction(SIGINT, &sigint_handler, nullptr);

    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    uint16_t port_number = (uint16_t) atoi(argv[1]);

    accept_file_descriptor = socket(AF_INET, SOCK_STREAM, 0);

    if (accept_file_descriptor < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    sockaddr_in serv_addr;

    /* Initialize socket structure */
    bzero((char *) &serv_addr, sizeof(serv_addr));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(port_number);

    if (bind(accept_file_descriptor, (sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR on binding");
        exit(1);
    }

    is_running = true;

    listen(accept_file_descriptor, 5);

    std::thread poll_thread(poll_loop);
    std::thread process_thread(process_loop);
    std::thread write_thread(write_loop);

    while (is_running) {
        sockaddr_in client_address;
        unsigned int client_length = sizeof(client_address);
        int client_socket = accept(accept_file_descriptor, (sockaddr *) &client_address, &client_length);
        if (client_socket < 0) {
            perror("ERROR on accept");
        } else {
            clients_mutex.lock();
            clients.emplace_back(client_socket);
            clients_condition.notify_all();
            clients_mutex.unlock();
            printf("Connected %d\n", client_socket);
            fflush(stdout);
        }
    }
    return 0;
}
