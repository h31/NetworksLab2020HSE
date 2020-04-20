#ifndef SERVER_CLIENT_H
#define SERVER_CLIENT_H

class Server;

class Client {
    int sockfd;
    Server* server;

public:
    void Serve();
    Client(int socket, Server *srv);
    int GetSocket();
};

#endif //SERVER_CLIENT_H
