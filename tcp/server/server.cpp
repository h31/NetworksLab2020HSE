#include "server.h"

#include <stdio.h>
#include <stdlib.h>

#include <netinet/in.h>
#include <unistd.h>

#include <string.h>

#include "client.h"
#include <thread>
#include <iostream>

Server::Server(int port) {
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    bzero((char *) &serv_addr, sizeof(serv_addr));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(port);

    if (bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR on binding");
        exit(1);
    }

    listen(sockfd, CLIENT_LIMIT);
}

void Server::Serve() {
    std::cerr << "server started" << std::endl;

    while(true) {
        int newsockfd;
        unsigned int clilen;
        sockaddr_in cli_addr;

        clilen = sizeof(cli_addr);
        newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
        if (newsockfd < 0) {
            perror("ERROR on accept");
            exit(1);
        }

        clients.push_back(std::make_unique<Client>(Client(newsockfd, this)));
        threads.push_back(std::make_unique<std::thread>(&Client::Serve, clients.back().get()));
    }
}

void Server::Notify(std::string msg) {
    for (int i = 0; i < clients.size(); i++) {
        int socket = clients[i].get()->GetSocket();
        ssize_t n = write(socket, msg.data(), msg.length()); // send on Windows
        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
    }
}