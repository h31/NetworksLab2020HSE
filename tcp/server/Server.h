#ifndef SERVER_H
#define SERVER_H

#include <netdb.h>
#include <list>
#include <poll.h>
#include <unordered_map>
#include <vector>
#include "Client.h"
#include "message.h"

class Server {
public:
    explicit Server(uint16_t port, int maxClientsNumber = 100);

    ~Server();

    void newMessage(const Message& message);

    void removeClient(const Client* client);

    void unregisterWrite(int socketFD);

private:
    int m_sockFD;
    sockaddr_in m_serverAddress{};
    std::unordered_map<int, std::shared_ptr<Client>> m_clients{};
    std::vector<pollfd> m_fds{};
    volatile bool m_stop = false;
    std::thread m_thread;

    void run();

    void acceptClients();

    std::vector<std::shared_ptr<Client>> readyReadClients();

    std::vector<std::shared_ptr<Client>> readyWriteClient();

    void registerRead(int socketFD);
};

#endif
