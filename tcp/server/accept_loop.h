#ifndef ACCEPT_LOOP_H
#define ACCEPT_LOOP_H

#include <thread>
#include "client_loop.h"
#include "client_container.h"

class AcceptLoop {
private:
    int socket_fd = 0;
    ClientContainer container;
    std::thread accept_thread;

public:
    void start(int socket_fd);

    void stop();

private:
    void loop();
};

#endif
