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

    int socketFD() const;

    void send(const Message& message, const std::shared_ptr<Client>& client);

    void shutdown();

private:
    const int m_socketFD = 0;
    Server* m_server;
    std::thread m_thread;
    ThreadPool m_pool{1};
    bool stop = false;

    void run();
};

#endif
