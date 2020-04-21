#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <string.h>
#include <pthread.h>
#include <string>
#include <vector>
#include <ctime>
#include <atomic>

static std::atomic<bool> stopped(false);

void *read_server_messages(void *arg) {
    char buffer[256];
    int sockfd = *(int *) arg;
    while (true) {
        if (stopped) {
            return nullptr;
        }
        bzero(buffer, 256);

        std::vector<char> server_message;
        static const int header_size = 2 * sizeof(uint32_t) + sizeof(time_t);
        while (server_message.size() < header_size) {
            if (stopped) {
                return nullptr;
            }
            int n = read(sockfd, buffer, 255);
            if (n < 0) {
                perror("ERROR reading from socket");
                exit(1);
            }
            server_message.insert(server_message.end(), buffer, buffer + n);
        }
        time_t time = *((time_t*) server_message.data());
        uint32_t message_length = *((uint32_t*) &server_message.data()[sizeof(time_t)]);

        uint32_t username_length = *((uint32_t*) &server_message.data()[sizeof(time_t) + sizeof (uint32_t)]);
        while (server_message.size() < header_size + message_length + username_length) {
            if (stopped) {
                return nullptr;
            }
            int n = read(sockfd, buffer, 255);
            if (n < 0) {
                perror("ERROR reading from socket");
                exit(1);
            }
            server_message.insert(server_message.end(), buffer, buffer + n);
        }

        std::vector<char> text_message = std::vector<char>(server_message.begin() + header_size,
                                                           server_message.begin() + header_size + message_length);
        text_message.push_back('\0');
        std::vector<char> username = std::vector<char>(server_message.begin() + header_size + message_length,
                                                       server_message.begin() + header_size + message_length + username_length);
        username.push_back('\0');
        std::tm * ptm = std::localtime(&time);
        char time_buffer[32];
        std::strftime(time_buffer, 32, "%H:%M", ptm);
        printf("<%s> [%s] %s\n", time_buffer, username.data(), text_message.data());
    }
    return nullptr;
}

void send_message_to_server(int sockfd, char *message, char* username) {
    uint32_t message_length = strlen(message);
    uint32_t username_length = strlen(username);
    std::vector<char> buffer;
    buffer.insert(buffer.end(), (char *) &message_length, ((char *) &message_length) + sizeof(message_length));
    buffer.insert(buffer.end(), (char *) &username_length, ((char *) &username_length) + sizeof(username_length));
    buffer.insert(buffer.end(), message, message + message_length);
    buffer.insert(buffer.end(), username, username + username_length);
    int n = write(sockfd, buffer.data(), buffer.size());
    if (n < 0) {
        perror("ERROR reading from socket");
        exit(1);
    }
}

// reads messages from the client
void client_loop(int sockfd, char *username) {
    char buffer[256];
    while (true) {
        bzero(buffer, 256);
        fgets(buffer, 255, stdin);
        if (feof(stdin)) {
            stopped = true;
            return;
        }
        if (buffer[0] == '\0') {
            continue;
        }
        int len = strlen(buffer);
        if (buffer[len - 1] == '\n') {
            buffer[len - 1] = '\0';
        }
        send_message_to_server(sockfd, buffer, username);
    }
}

int main(int argc, char *argv[]) {
    int sockfd, err;

    if (argc < 4) {
        fprintf(stderr, "usage %s hostname port username\n", argv[0]);
        exit(0);
    }

    char* hostname = argv[1];
    char* port = argv[2];
    char* username = argv[3];

    struct addrinfo hints = {}, *addrs;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    err = getaddrinfo(hostname, port, &hints, &addrs);
    if (err != 0)
    {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    for(struct addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next)
    {
        sockfd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (sockfd == -1)
        {
            err = errno;
            break;
        }

        if (connect(sockfd, addr->ai_addr, addr->ai_addrlen) == 0)
        {
            break;
        }

        err = errno;

        close(sockfd);
        sockfd = -1;
    }

    freeaddrinfo(addrs);

    if (sockfd == -1)
    {
        fprintf(stderr, "%s: %s\n", hostname, strerror(err));
        exit(1);
    }

    pthread_t thread;
    pthread_create(&thread, nullptr, read_server_messages, (void*) &sockfd);
    client_loop(sockfd, username);
    pthread_cancel(thread);
    return 0;
}
