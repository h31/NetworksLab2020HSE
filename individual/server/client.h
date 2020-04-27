#ifndef SERVER_CLIENT_H
#define SERVER_CLIENT_H


#include <netinet/in.h>
#include <thread>
#include <unistd.h>
#include <poll.h>
#include <queue>
#include "message.h"

class server;

class client {
private:
    pollfd fd;
    server* server_ptr;
    message input;
    std::queue<message> output;

public:
    client(int fd, server* server_ptr) : fd(pollfd{fd, POLLIN, 0}), server_ptr(server_ptr) {}

    int read();

    pollfd get_fd() const {
        return fd;
    }

    int write();

    void put_msg(const message& msg);
    
    message get_msg();

    ~client() {
        std::cout << "Shutdown client on socket:" << fd.fd;
        close(fd.fd);
    }

    bool ready();

};


#endif //SERVER_CLIENT_H
