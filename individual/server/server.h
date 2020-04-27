#ifndef SERVER_SERVER_H
#define SERVER_SERVER_H

#include <netinet/in.h>
#include <sys/poll.h>
#include <unordered_map>
#include <atomic>
#include "client.h"
#include "message.h"


class server {

private:
    uint16_t port;
    std::atomic<bool> running;
    pollfd accept_fd;
    std::thread loop_thread;
    std::unordered_map<int, std::shared_ptr<client>> clients;

public:

    server(uint16_t port, int fd) : port(port), running(false), accept_fd(pollfd{fd, POLLIN, 0}) {};

    void stop();

    void start();

    void write_all(const message& msg);

    ~server() {
        stop();
    }

private:

    void loop();

    void write(int fd);

    void read(int fd);

    void remove_client(int fd);

    void accept_clients();
};


#endif //SERVER_SERVER_H
