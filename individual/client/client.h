#ifndef CLIENT_CLIENT_H
#define CLIENT_CLIENT_H


#include <string>
#include <atomic>
#include <thread>
#include <netinet/in.h>
#include <netdb.h>
#include "socket_io.h"
class client {

public:
    std::atomic<bool> running;

private:
    std::string name;
    int fd;
    socket_io io;
    std::thread  read_thread;
    std::thread  write_thread;

    void read_loop();

    void write_loop();

public:
    client(const char* name, const char* hostname, const char* port) : running(false), name(name) {
        int err;
        struct addrinfo hints = {}, *addrs;
        hints.ai_family = AF_INET;
        hints.ai_socktype = SOCK_STREAM;
        hints.ai_protocol = IPPROTO_TCP;

        err = getaddrinfo(hostname, port, &hints, &addrs); // gethostbyname is deprecated
        if (err != 0)
        {
            std::cout << "Error on getaddrino:" << gai_strerror(err);
            exit(1);
        }

        for (struct addrinfo *addr = addrs; addr != NULL; addr = addr->ai_next)
        {
            fd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
            if (fd == -1)
            {
                err = errno;
                break;
            }
            if (connect(fd, addr->ai_addr, addr->ai_addrlen) == 0)
            {
                break;
            }
            err = errno;
            close(fd);
            fd = -1;
        }

        freeaddrinfo(addrs);

        if (fd == -1)
        {
            std::cout << "Error on freeaddrino:" << strerror(err);
            exit(1);
        }

        io = socket_io(fd);
    }

    void start();

    void stop();

    ~client() {
        stop();
    }
};


#endif //CLIENT_CLIENT_H
