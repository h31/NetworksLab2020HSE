#include <strings.h>
#include <cstdio>
#include <cstdlib>
#include <unistd.h>
#include <iostream>
#include <mutex>
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

        //std::cerr << buffer.data() << std::endl;
        std::string msg = readMsg(sockfd, len);
        if (msg == "") {
            break;
        }
        server->Notify(&msg);
    }
}

std::string readMsg(int socket, int len) {
    std::vector<char> buffer(len + 1, 0);
    int got = 0;

    while (got < len) {
        int n = read(socket, buffer.data() + got, len - got);
        if (n == 0) {
            return "";
        }
        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        got += n;
    }

    return buffer.data();
}

int Client::GetSocket() {
    return sockfd;
}

std::mutex mu;

void Client::Notify(std::string msg) {
    mu.lock();
    int n = write(sockfd, std::to_string(msg.length()).data(), 4);
    if (n < 0) {
        perror("ERROR writing to socket");
        exit(1);
    }
    n = write(sockfd, msg.data(), msg.length());
    if (n < 0) {
        perror("ERROR writing to socket");
        exit(1);
    }
    mu.unlock();
}