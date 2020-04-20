#ifndef SERVER_CLIENT_H
#define SERVER_CLIENT_H


#include <netinet/in.h>
#include <thread>
#include <boost/asio/thread_pool.hpp>
#include <boost/thread.hpp>
#include <unistd.h>
#include "socket_io.h"

class chat;

class client {
private:
    std::atomic<bool> running;
    int socket_fd;
    socket_io io;
    chat* chat_ptr;
    boost::thread read_thread;
    boost::asio::thread_pool write_thread;

    void loop();

public:
    client(int socket_fd, chat* chat_ptr) : socket_fd(socket_fd),
                                            io(socket_fd),
                                            chat_ptr(chat_ptr),
                                            running(true),
                                            read_thread(&client::loop, this),
                                            write_thread(1) {}

    bool operator==(const client& other) const
    {
        return socket_fd == other.socket_fd;
    }

    void send(const message& msg);

    void shutdown();

    ~client() {
        shutdown();
    }
};


#endif //SERVER_CLIENT_H
