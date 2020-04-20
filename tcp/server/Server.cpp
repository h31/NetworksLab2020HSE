//
// Created by ralsei on 20.04.2020.
//

#include <algorithm>
#include "Server.h"

Server::Server(uint32_t port) {
    pthread_mutex_init(broadcast_mutex, nullptr);

    socketDescriptor = socket(AF_INET, SOCK_STREAM, 0);
    if (socketDescriptor < 0) {
        leave("ERROR opening socket");
        return;
    }

    bzero((char *) &serverAddress, sizeof(serverAddress));
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_addr.s_addr = INADDR_ANY;
    serverAddress.sin_port = htons(port);

    if (bind(socketDescriptor, (struct sockaddr *) &serverAddress, sizeof(serverAddress)) < 0) {
        leave("ERROR on binding");
        return;
    }

    listen(socketDescriptor, 100);
    clientAcceptThread = std::thread(&Server::clientAccepting, this);
}

void Server::clientAccepting() {
    while (working) {
        if (!working) {
            return;
        }

        struct sockaddr_in cli_addr{};
        uint32_t clilen = sizeof(cli_addr);
        int newsockfd = accept(socketDescriptor, (struct sockaddr *) &cli_addr, &clilen);

        if (newsockfd < 0) {
            leave("ERROR on accept");
            return;
        }

        std::cout << "Client accepted" << std::endl;
        auto client = new Client(newsockfd, this);
        addClient(client);
    }
}

void Server::addClient(Client *client) {
    clientMutex.lock();
    clients.push_back(client);
    clientMutex.unlock();
}

Server::~Server() {
    working = false;

    shutdown(socketDescriptor, SHUT_RDWR);
    close(socketDescriptor);

    clientAcceptThread.join();

    for (auto &client : clients) {
        delete client;
    }

    pthread_mutex_destroy(broadcast_mutex);
    delete broadcast_mutex;
}

void Server::removeClient(Client *client) {
    clientMutex.lock();

    for (uint32_t i = 0; i < clients.size(); i++) {
        if (clients[i] == client) {
            delete clients[i];
            clients.erase(clients.begin() + i);
            break;
        }
    }

    clientMutex.unlock();
}

void Server::broadcastToAllClients(Message *message) {
    clientMutex.lock();
    pthread_mutex_lock(broadcast_mutex);
    for (auto client: clients) {
        message->writeToClientSocket(client->getSocketDescriptor());
    }
    pthread_mutex_unlock(broadcast_mutex);
    clientMutex.unlock();
}

void Server::leave(const char *message) {
    if (working) {
        perror(message);
        exit(1);
    }
}
