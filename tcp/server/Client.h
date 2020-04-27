//
// Created by ralsei on 20.04.2020.
//

#ifndef SERVER_CLIENT_H
#define SERVER_CLIENT_H

#include <memory>
#include <unistd.h>
#include <cstring>
#include <ctime>
#include <netinet/in.h>
#include <iomanip>
#include <vector>
#include <iostream>
#include <queue>
#include "Message.h"
#include "Server.h"

class Server;

class Client {
public:
    Client(int socketDescriptor, Server *server);

    ~Client();

    int getSocketDescriptor() {
        return socketDescriptor;
    }

    void readingMessages();

    void writingMessages();

    void submitNewMessageToQueue(Message *message);

private:
    Message* currentMessage;

    int socketDescriptor;
    Server *server;

    std::queue<Message *> messagesToSend;

};


#endif //SERVER_CLIENT_H
