//
// Created by ralsei on 20.04.2020.
//

#ifndef SERVER_CLIENT_H
#define SERVER_CLIENT_H

#include "../Message.h"
#include "Server.h"
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
#include <queue>

class Server;

/**
 * For each accepting client, holds two threads: one that reads input messages from the client and submit it to another thread,
 * which broadcast input messages for all clients.
 */
class Client {
public:
    Client(int socketDescriptor, Server *server);

    ~Client();

    int getSocketDescriptor() {
        return socketDescriptor;
    }

private:
    int socketDescriptor;
    Server *server;

    /**
     * True if server wants to stop the work and asks this client to finish
     */
    volatile bool interrupted = false;

    std::thread readFromServerThread;
    std::thread broadcastToAllClientsThread;

    pthread_mutex_t *messagesQueueMutex = new pthread_mutex_t();
    pthread_cond_t *queueNotEmpty = new pthread_cond_t();

    std::queue<Message *> messagesToSend;

    void readingMessages();

    void writingMessages();

    void submitNewMessageToQueue(Message *message);

    bool isWorking() const;
};


#endif //SERVER_CLIENT_H
