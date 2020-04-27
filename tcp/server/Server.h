//
// Created by ralsei on 20.04.2020.
//

#ifndef SERVER_SERVER_H
#define SERVER_SERVER_H

#include <memory>
#include <unistd.h>
#include <cstring>
#include <ctime>
#include <netinet/in.h>
#include <iomanip>
#include <vector>
#include <mutex>
#include <iostream>
#include <algorithm>
#include <sys/poll.h>
#include "Client.h"
#include "Message.h"

class Client;

class Server {
public:
    explicit Server(uint32_t port);

    void broadcastToAllClients(Message *message);

    void addClient(Client *client);

    void removeClient(Client *client);

    ~Server();

    void removeWriteRegistration(Client *client);

private:
    int socketDescriptor;
    sockaddr_in serverAddress{};

    int numberOfAdditionalDescriptors = 1;
    std::vector<Client *> clients;
    std::vector<pollfd> pollDescriptors{};

    volatile bool working = true;

    void clientAccepting();

    void leave(const char *message);

    void routine();
};


#endif //SERVER_SERVER_H
