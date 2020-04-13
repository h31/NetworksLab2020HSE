#ifndef CLIENT_H
#define CLIENT_H

#include <thread>
#include "message.h"
#include "ThreadPool.h"

class Server;

class Client {

public:
    Client(int socketFD, Server* server);

    Client(const Client& other) = delete;

    ~Client();

    void send(const Message& message);

private:
    const int m_socketFD = 0;
    Server* m_server;
    const std::thread m_thread;
    ThreadPool m_pool{1};

    void run();
};

#endif
