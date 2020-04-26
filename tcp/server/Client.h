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

    void send(const Message& message);

    void read();

    void write();

    void shutdown();

private:
    const int m_socketFD = 0;
    Server* m_server;
    std::queue<Message> m_messages;
    char m_sizeBuffer[4]{};
    std::size_t m_writePosition = 0;
    std::size_t m_readPosition = 0;
    std::vector<Message> m_readBuffer;
};

#endif
