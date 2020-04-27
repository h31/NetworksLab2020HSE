#include <stdio.h>
#include <stdlib.h>

#include <fcntl.h>
#include <poll.h>
#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <string>
#include <vector>
#include <ctime>
#include <atomic>

struct Client {
    int sockfd;
    std::vector<char> output_buffer;
};

std::vector<Client> clients;

// writes message to output buffers of all the clients and add POLLOUT to their events
void write_message_to_all_output_buffers(const char *text_message, const char *username) {

    uint32_t message_length = strlen(text_message);
    uint32_t username_length = strlen(username);
    std::time_t cur_time = std::time(nullptr);

    std::vector<char> buffer;
    buffer.insert(buffer.end(), (char *) &cur_time, ((char *) &cur_time) + sizeof(cur_time));
    buffer.insert(buffer.end(), (char *) &message_length, ((char *) &message_length) + sizeof(message_length));
    buffer.insert(buffer.end(), (char *) &username_length, ((char *) &username_length) + sizeof(username_length));
    buffer.insert(buffer.end(), text_message, text_message + message_length);
    buffer.insert(buffer.end(), username, username + username_length);

    for (Client &client : clients) {
        client.output_buffer.insert(client.output_buffer.end(), buffer.begin(), buffer.end());
    }
}

// reads the message from the client and writes it to output buffers of all the clients
void process_client(Client &client) {
    char buffer[256];
    bzero(buffer, 256);
    std::vector<char> client_message;
    static const int header_size = 2 * sizeof (uint32_t);

    // read header to get sizes of the message and the username
    while (client_message.size() < header_size) {
        int n = read(client.sockfd, buffer, 255);
        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        client_message.insert(client_message.end(), buffer, buffer + n);
    }
    uint32_t message_length = *((uint32_t*) client_message.data());
    uint32_t username_length = *((uint32_t*) &client_message.data()[sizeof(uint32_t)]);

    // read the rest of the message
    while (client_message.size() < header_size + message_length + username_length) {
        int n = read(client.sockfd, buffer, 255);
        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        client_message.insert(client_message.end(), buffer, buffer + n);
    }
    std::vector<char> text_message = std::vector<char>(client_message.begin() + header_size,
                                                       client_message.begin() + header_size + message_length);
    text_message.push_back('\0');
    std::vector<char> username = std::vector<char>(client_message.begin() + header_size + message_length,
                                                   client_message.begin() + header_size + message_length + username_length);
    username.push_back('\0');

    printf("[%s] %s\n", username.data(), text_message.data());

    write_message_to_all_output_buffers(text_message.data(), username.data());

}

void accept_new_client(int server_sockfd, std::vector<pollfd> &fds) {
    sockaddr_in cli_addr;
    unsigned int clilen = sizeof(cli_addr);
    int newsockfd = accept(server_sockfd, (struct sockaddr *) &cli_addr, &clilen);
    if (newsockfd < 0) {
        perror("ERROR on accept");
        exit(1);
    }
    fcntl(newsockfd, F_SETFL, O_NONBLOCK);
    pollfd newfd;
    newfd.fd = newsockfd;
    newfd.events = POLLIN;
    fds.push_back(newfd);
    Client client;
    client.sockfd = newsockfd;
    clients.push_back(client);
}

void write_client_output_buffer(Client &client) {
    int n = write(client.sockfd, client.output_buffer.data(), client.output_buffer.size());
    if (n < 0) {
        perror("ERROR on write");
        exit(1);
    } else {
        client.output_buffer.erase(client.output_buffer.begin(), client.output_buffer.begin() + n);
    }
}

void set_clients_events(std::vector<pollfd> &fds) {
    for (size_t i = 2; i < fds.size(); i++) {
        if (!clients[i - 2].output_buffer.empty()) {
            fds[i].events = POLLIN | POLLOUT;
        } else {
            fds[i].events = POLLIN;
        }
    }
}

// main server method
void server_loop(int server_sockfd) {
    std::vector<pollfd> fds;

    // add pollfd for server to accept new clients
    pollfd serverfd;
    serverfd.fd = server_sockfd;
    serverfd.events = POLLIN;
    fds.push_back(serverfd);
    const int server_idx = 0;

    // add pollfd for stdin
    fcntl(STDIN_FILENO, F_SETFL, O_NONBLOCK);
    pollfd stdinfd;
    stdinfd.fd = STDIN_FILENO;
    stdinfd.events = POLLIN;
    fds.push_back(stdinfd);
    const int stdin_idx = 1;

    while (true) {
        int res = poll(fds.data(), fds.size(), -1);
        if (res < 0) {
            perror("ERROR while calling poll");
            exit(1);
        }

        if (fds[stdin_idx].revents & POLLIN) {
            char c = getc(stdin);
            if (c == EOF) {
                break;
            }
        }

        if (fds[server_idx].revents & POLLIN) {
            accept_new_client(server_sockfd, fds);
        }

        for (size_t i = 2; i < fds.size(); i++) {
            Client &client = clients[i - 2];
            if (fds[i].revents & POLLIN) {
                process_client(client);
            }
            if (fds[i].revents & POLLOUT) {
                write_client_output_buffer(client);
            }
        }

        set_clients_events(fds);

    }

    close(server_sockfd);
    char buffer[256];
    for (Client &client : clients) {
        shutdown(client.sockfd, SHUT_WR);
        while (read(client.sockfd, buffer, 255) > 0);
        close(client.sockfd);
    }
}

int main(int argc, char *argv[]) {
    int sockfd;
    uint16_t portno;
    struct sockaddr_in serv_addr;

    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

    if (sockfd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    portno = (uint16_t) atoi(argv[1]);

    bzero((char *) &serv_addr, sizeof(serv_addr));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);

    if (bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR on binding");
        exit(1);
    }

    fcntl(sockfd, F_SETFL, O_NONBLOCK);
    listen(sockfd, 5);
    server_loop(sockfd);
    close(sockfd);
    return 0;
}
