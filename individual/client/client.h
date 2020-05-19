#ifndef CLIENT_CLIENT_H
#define CLIENT_CLIENT_H


#include <string>
#include <atomic>
#include "socket_io.h"
#include <boost/thread.hpp>
class client {

public:
    std::atomic<bool> running;
private:
    std::string name;
    int socket_fd;
    socket_io io;
    boost::thread  read_thread;
    boost::thread  write_thread;

public:
    client(const char* name, int socket_fd) :   name(name),
                                                running(true),
                                                socket_fd(socket_fd),
                                                io(socket_fd),
                                                read_thread(&client::read_loop, this),
                                                write_thread(&client::write_loop, this) {}

    ~client() {
        shutdown();
    }

private:

    void read_loop();

    void write_loop();

    void shutdown();
};


#endif //CLIENT_CLIENT_H
