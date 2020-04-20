//
// Created by ralsei on 20.04.2020.
//

#ifndef CLIENT_CLIENT_H
#define CLIENT_CLIENT_H

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
#include <queue>

class Server;

class Client {
public:
    Client(char* hostname, char* port, std::string name);

    ~Client();

private:
    std::string name;
    int serverSocketDescriptor;

    volatile bool interrupted = false;

    std::thread readFromConsoleThread;
    std::thread writeToServerThread;
    std::thread readFromServerThread;

    pthread_mutex_t *messagesQueueMutex = new pthread_mutex_t();
    pthread_cond_t *queueNotEmpty = new pthread_cond_t();

    std::queue<Message*> messagesToSend;

    void readingMessagesFromServer();

    void readingMessagesFromConsole();

    void writingMessages();

    void submitNewMessage(Message *message);

    bool isWorking() const;

    void leave();
};

#endif //CLIENT_CLIENT_H
