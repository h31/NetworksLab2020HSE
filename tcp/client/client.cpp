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

    for(struct addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next) {
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
    char buffer[256];
    char lengthBuf[4];
    /* Now read server response */
    while (true) {
        bzero(lengthBuf, 4);
        bzero(buffer, 256);
        int n = read(sockfd, buffer, 255);
        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        if (n == 0) {
            break;
        }
        std::cerr << buffer << std::endl;
    }
}

void Client::listenToUser() {
    //char buffer[256];

    //printf("Please enter the message: ");
    while (true) {
        std::cerr << "user while\n";
        //bzero(buffer, 256);
        //fgets(buffer, 255, stdin);

        std::string input;
        std::getline(std::cin, input);

        /* Send message to the server */
        //int n = write(sockfd, buffer, strlen(buffer));

        std::cerr << "input: " << input << std::endl;
        int n = write(sockfd, input.data(), input.length());
        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
    }
}