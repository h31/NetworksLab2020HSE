#ifndef SERVER_H
#define SERVER_H

#include <netdb.h>
#include <list>
#include "Client.h"
#include "message.h"

class Server {
public:
    explicit Server(uint16_t port, int maxClientsNumber = 100);

    ~Server();

    void run();

    void newMessage(const Message& message);

    void removeClient(const Client* client);

private:
    int m_sockFD;
    sockaddr_in m_serverAddress{};
    std::list<std::shared_ptr<Client>> m_clients;
    std::mutex m_mutex;
};

#endif
