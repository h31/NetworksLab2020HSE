#ifndef CLIENT_CLIENT_H
#define CLIENT_CLIENT_H


#include <string>

std::string readMsg(int socket, int len);

class Client {
    int sockfd;
    std::string username;

    void listenToServer();
    void listenToUser();
public:
    Client(char* hostname, char* port, char* username);
    void Run();
};


#endif //CLIENT_CLIENT_H
