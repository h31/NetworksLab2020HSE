#include <strings.h>
#include <cstdio>
#include <cstdlib>
#include <zconf.h>
#include <iostream>
#include "client.h"
#include "server.h"

Client::Client(int socket, Server* srv) {
    sockfd = socket;
    server = srv;
    std::cerr << "client created" << std::endl;
}

void Client::Serve() {
    const int BUF_SIZE = 256;
    std::vector<char> buffer(BUF_SIZE, 0);

    while (true) {
        std::fill(buffer.begin(), buffer.end(), 0);
        ssize_t n = read(sockfd, buffer.data(), BUF_SIZE - 1); // recv on Windows
        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        if (n == 0) {
            break;
        }
        printf("Here is the message: %s\n", buffer.data());

        server->Notify(buffer.data());
    }
}


int Client::GetSocket() {
    return sockfd;
}