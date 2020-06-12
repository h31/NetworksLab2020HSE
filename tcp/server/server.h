#ifndef SERVER_H
#define SERVER_H

#include <cstdint>
#include "accept_loop.h"

class ChatServer {
private:
    uint16_t port;
    AcceptLoop accept_loop;
public:
    explicit ChatServer(uint16_t port);

    void start();

    void stop();
};

#endif
