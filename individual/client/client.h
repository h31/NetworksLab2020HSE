#ifndef CLIENT_CLIENT_H
#define CLIENT_CLIENT_H


#include <string>
#include <atomic>
#include <thread>
#include "socket_io.h"
class client {

public:
    std::atomic<bool> running;

private:
    std::string name;
    int socket_fd;
    socket_io io;
    std::thread  read_thread;
    std::thread  write_thread;

    void read_loop();

    void write_loop();

    void shutdown();

public:
    client(const char* name, int socket_fd) :   running(true),
                                                name(name),
                                                socket_fd(socket_fd),
                                                io(socket_fd),
                                                read_thread(&client::read_loop, this),
                                                write_thread(&client::write_loop, this) {}

    ~client() {
        shutdown();
    }
};


#endif //CLIENT_CLIENT_H
