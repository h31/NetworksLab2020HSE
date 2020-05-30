#ifndef SERVER_SERVER_H
#define SERVER_SERVER_H

#include <netdb.h>
#include "vector"
#include "client.h"
#include <memory>
#include <thread>

const int CLIENT_LIMIT = 10;

class Server {
    int sockfd;
    struct sockaddr_in serv_addr;
    std::vector<std::unique_ptr<Client>> clients;
    std::vector<std::unique_ptr<std::thread>> threads;

public:
    Server(int port);
    void Serve();
    void Notify(std::string* msg);
};


#endif //SERVER_SERVER_H
