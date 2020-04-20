//
// Created by ralsei on 20.04.2020.
//

#ifndef SERVER_SERVER_H
#define SERVER_SERVER_H

#include "../Message.h"
#include <memory>
#include <unistd.h>
#include <cstring>
#include <ctime>
#include <netinet/in.h>
#include <iomanip>
#include <vector>
#include <thread>
#include <mutex>
#include <iostream>
#include "Client.h"

class Client;

class Server {
public:
    explicit Server(uint32_t port);

    void broadcastToAllClients(Message *message);

    void addClient(Client *client);

    void removeClient(Client *client);

    ~Server();

private:
    int socketDescriptor;
    sockaddr_in serverAddress{};
    std::vector<Client *> clients;

    std::mutex clientMutex;
    pthread_mutex_t *broadcast_mutex = new pthread_mutex_t();
    volatile bool working = true;
    std::thread clientAcceptThread;

    void clientAccepting();

    void leave(const char *message);
};


#endif //SERVER_SERVER_H
