#ifndef SERVER_CLIENT_H
#define SERVER_CLIENT_H

#include <string>

class Server;

class Client {
    int sockfd;
    Server* server;

public:
    void Serve();
    Client(int socket, Server *srv);
    int GetSocket();
    void Notify(std::string msg);
};

#endif //SERVER_CLIENT_H
