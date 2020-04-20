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

std::atomic_bool is_running;
std::atomic_int accept_file_descriptor;
std::mutex clients_mutex;
std::vector<int> clients_descriptors;

void sigint_handler_callback(int signum) {
    is_running = false;
}

int readn(int fd, char *buf, int count) {
    int read_count = 0;
    while (count != 0) {
        int n = read(fd, buf, count);
        read_count += n;
        if (n < 0) {
            perror("ERROR reading from client_socket");
            return n;
        } else if (n == 0) {
            perror("ERROR EOF found");
            return n;
        }
        count -= n;
        buf += n;
    }
    return read_count;
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

std::pair<std::string, bool> read_message(int client_file_descriptor) {
    char length_buf[4];
    bzero(length_buf, 4);
    int n = readn(client_file_descriptor, length_buf, 4);
    if (n == 0) {
        perror("READ 0 bytes");
        return {"", false};
    }
    uint32_t length = ntohl(*(uint32_t *) length_buf);
    printf("READ %d length\n", length);
    std::string message;
    message.resize(length);
    n = readn(client_file_descriptor, &message[0], length); // dangerous usage of &message[0]
    if (n == 0) {
        perror("READ 0 bytes");
        return {"", false};
    }
    return {message, true};
}

void client_loop(int client_file_descriptor) {
    while (is_running) {
        auto res = read_message(client_file_descriptor);
        auto message = res.first;
        printf("Got message from %d: %s\n", client_file_descriptor, message.c_str());
        fflush(stdout);
        message = "MESSAGE FROM " + std::to_string(client_file_descriptor) + ": " + message;
        clients_mutex.lock();
        for (auto client_fd : clients_descriptors) {
            int n = writen(client_fd, message.c_str(), message.size());
            if (n < 0) {
                perror("ERROR writing to socket");
                exit(1);
            }
        }
        clients_mutex.unlock();
        if (!res.second) {
            break;
        }
    }
    printf("Close connection with %d", client_file_descriptor);
    fflush(stdout);
    clients_mutex.lock();
    close(client_file_descriptor);
    clients_descriptors.erase(
            std::find(clients_descriptors.begin(), clients_descriptors.end(), client_file_descriptor));
    clients_mutex.unlock();
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

    std::vector<std::thread> clients;
    while (is_running) {
        sockaddr_in client_address;
        unsigned int client_length = sizeof(client_address);
        int client_socket = accept(accept_file_descriptor, (sockaddr *) &client_address, &client_length);
        if (client_socket < 0) {
            perror("ERROR on accept");
        } else {
            clients_mutex.lock();
            clients_descriptors.push_back(client_socket);
            clients_mutex.unlock();
            printf("Connected %d\n", client_socket);
            fflush(stdout);
            clients.push_back(std::thread(client_loop, client_socket));
        }
    }

    for (auto &client : clients) {
        client.join();
    }
    return 0;
}
