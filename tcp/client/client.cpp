#include "client.h"

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <string.h>

#include <stdio.h>
#include <stdlib.h>
#include <thread>
#include <iostream>
#include <vector>

#include <time.h>

Client::Client(char* hostname, char* port, char* username) {
    this->username = username;

    struct addrinfo hints = {}, *addrs;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    int err = getaddrinfo(hostname, port, &hints, &addrs); // gethostbyname is deprecated
    if (err != 0) {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    for (struct addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next) {
        sockfd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (sockfd == -1) {
            err = errno;
            break;
        }
        if (connect(sockfd, addr->ai_addr, addr->ai_addrlen) == 0) {
            break;
        }
        err = errno;
        close(sockfd);
        sockfd = -1;
    }
    freeaddrinfo(addrs);
    if (sockfd == -1) {
        fprintf(stderr, "%s: %s\n", hostname, strerror(err));
        exit(1);
    }
}

void Client::Run() {
    std::thread srvl(&Client::listenToServer, this);
    std::thread usrl(&Client::listenToUser, this);
    srvl.join();
    usrl.join();
}

void Client::listenToServer() {
    char lenBuf[4];
    while (true) {
        std::cerr << "start listening\n";
        bzero(lenBuf, 4);
        int n = read(sockfd, lenBuf, 4);
        if (n == 0) {
            break;
        }

        int len = strtol(lenBuf, nullptr, 10);
        std::vector<char> buffer(len + 1, 0);
        n = read(sockfd, buffer.data(), len);
        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        if (n == 0) {
            break;
        }
        std::cout << buffer.data() << std::endl;
    }
}

void Client::listenToUser() {
    while (true) {
        std::string input;
        time_t timer;
        std::getline(std::cin, input);
        time(&timer);

        //std::cerr << "input: " << input << std::endl;
        input = "<" + std::to_string(timer) + "> [" + username + "]: " + input;
        int n = write(sockfd, std::to_string(input.length()).data(), 4);
        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
        n = write(sockfd, input.data(), input.length());
        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
    }
}