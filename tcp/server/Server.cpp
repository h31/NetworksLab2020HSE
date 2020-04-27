//
// Created by ralsei on 20.04.2020.
//

#include "Server.h"

Server::Server(uint32_t port) {
    socketDescriptor = socket(AF_INET, SOCK_STREAM, 0);
    if (socketDescriptor < 0) {
        leave("ERROR opening socket");
        return;
    }

    pollfd fd{};
    fd.events = POLLIN;
    fd.fd = socketDescriptor;
    pollDescriptors.push_back(fd);

    bzero((char *) &serverAddress, sizeof(serverAddress));
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_addr.s_addr = INADDR_ANY;
    serverAddress.sin_port = htons(port);

    if (bind(socketDescriptor, (struct sockaddr *) &serverAddress, sizeof(serverAddress)) < 0) {
        leave("ERROR on binding");
        return;
    }

    listen(socketDescriptor, 100);

    routine();
}

void Server::routine() {
    while (working) {
        if (!working) {
            return;
        }

        auto result = poll(pollDescriptors.data(), pollDescriptors.size(), -1);
        if (result < 0) {
            leave("ERROR polling");
        }

        if (pollDescriptors[0].revents & POLLIN) {
            clientAccepting();
        }

        for (int i = numberOfAdditionalDescriptors; i < pollDescriptors.size(); i++) {
            auto& descriptor = pollDescriptors[i];
            auto& client = clients[i - numberOfAdditionalDescriptors];

            if (descriptor.revents & POLLIN) {
                client->readingMessages();
            } else if (descriptor.revents & POLLOUT) {
                client->writingMessages();
            }
        }
    }
}

void Server::clientAccepting() {
    struct sockaddr_in cli_addr{};
    uint32_t clilen = sizeof(cli_addr);
    int newsockfd = accept(socketDescriptor, (struct sockaddr *) &cli_addr, &clilen);

    if (newsockfd < 0) {
        return;
    }

    std::cout << "Client accepted" << std::endl;
    auto client = new Client(newsockfd, this);
    addClient(client);
}

void Server::addClient(Client *client) {
    auto descriptor = client->getSocketDescriptor();
    clients.push_back(client);

    pollfd fd{};
    fd.events = POLLIN;
    fd.fd = descriptor;
    pollDescriptors.push_back(fd);
}

void Server::removeWriteRegistration(Client* client) {
    for (auto& descriptor: pollDescriptors) {
        if (descriptor.fd == client->getSocketDescriptor()) {
            if (descriptor.events & POLLOUT) {
                descriptor.events ^= POLLOUT;
            }
        }
    }
}

Server::~Server() {
    working = false;

    shutdown(socketDescriptor, SHUT_RDWR);
    close(socketDescriptor);

    for (auto &client : clients) {
        delete client;
    }
}

void Server::removeClient(Client *client) {
    for (uint32_t i = 0; i < clients.size(); i++) {
        if (clients[i] == client) {
            delete clients[i];
            clients.erase(clients.begin() + i);
            pollDescriptors.erase(pollDescriptors.begin() + i + numberOfAdditionalDescriptors);
            break;
        }
    }
}

void Server::broadcastToAllClients(Message *message) {
    for (uint32_t i = 0; i < clients.size(); i++) {
        auto& client = clients[i];
        auto& descriptor = pollDescriptors[i + numberOfAdditionalDescriptors];

        if ((descriptor.events & POLLOUT) == 0) {
            descriptor.events ^= POLLOUT;
        }

        client->submitNewMessageToQueue(new Message(message));
    }
}

void Server::leave(const char *message) {
    if (working) {
        perror(message);
        exit(1);
    }
}
