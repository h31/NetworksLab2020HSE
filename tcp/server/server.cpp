#include <cstdint>
#include <netinet/in.h>
#include <cstdio>
#include <cstdlib>
#include <strings.h>
#include <iostream>
#include "server.h"

ChatServer::ChatServer(uint16_t port) : port(port) {
}

void ChatServer::start() {
    int socket_fd;
    sockaddr_in server_address{};

    socket_fd = socket(AF_INET, SOCK_STREAM, 0);

    int enable = 1;
    if (setsockopt(socket_fd, SOL_SOCKET, SO_REUSEADDR, &enable, sizeof(int)) < 0) {
        perror("setsockopt(SO_REUSEADDR) failed");
        exit(1);
    }

    if (socket_fd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    bzero((char *) &server_address, sizeof(server_address));

    server_address.sin_family = AF_INET;
    server_address.sin_addr.s_addr = INADDR_ANY;
    server_address.sin_port = htons(port);

    if (bind(socket_fd, (struct sockaddr *) &server_address, sizeof(server_address)) < 0) {
        perror("ERROR on binding");
        exit(1);
    }

    listen(socket_fd, 5);

    accept_loop.start(socket_fd);
}

void ChatServer::stop() {
    accept_loop.stop();
}
