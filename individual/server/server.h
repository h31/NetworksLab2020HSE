#ifndef SERVER_SERVER_H
#define SERVER_SERVER_H


#include <netinet/in.h>
#include "client.h"
#include "chat.h"

class server {
private:
    std::atomic<bool> running;
    chat clients_chat;
    int socket_fd;
    uint16_t port;
    boost::thread accept_thread;

public:

    server(uint16_t port, int socket_fd) : port(port), socket_fd(socket_fd), running(true), accept_thread(&server::loop, this) {}

    void shutdown();

    ~server() {
        shutdown();
    }

private:

    void loop();

    int connect_client() const;
};


#endif //SERVER_SERVER_H
