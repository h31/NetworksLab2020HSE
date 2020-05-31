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
    char lenBuf[4];

    while (true) {
        bzero(lenBuf, 4);
        int n = read(sockfd, lenBuf, 4);
        int len = strtol(lenBuf, nullptr, 10);
        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }

        std::vector<char> buffer(len + 1, 0);
        n = read(sockfd, buffer.data(), len);
        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        if (n == 0) {
            break;
        }

        std::cerr << buffer.data() << std::endl;
        server->Notify((buffer.data()));
    }
}


int Client::GetSocket() {
    return sockfd;
}