#ifndef SERVER_SERVER_H
#define SERVER_SERVER_H

#include <netinet/in.h>
#include <sys/poll.h>
#include <unordered_map>
#include <atomic>
#include "client.h"
#include "message.h"


const int ON = 1;

class server {

private:
    uint16_t port;
    std::atomic<bool> running;
    pollfd accept_fd;
    sockaddr_in server_addr{};
    std::thread loop_thread;
    std::unordered_map<int, std::shared_ptr<client>> clients;

public:

    server(uint16_t port) : port(port), running(false) {
        int fd = socket(AF_INET, SOCK_STREAM, 0);

        if (fd < 0) {
            std::cerr << "Error opening socket socket_fd:" << fd << std::endl;
            exit(1);
        }

        if (setsockopt(fd, SOL_SOCKET,  SO_REUSEADDR, (char *)&ON, sizeof(ON)) < 0)
        {
            std::cerr << "Error in allowing socket be reusable socket_fd:" << fd;
            close(fd);
            exit(-1);
        }

        if (ioctl(fd, FIONBIO, (char *)&ON) < 0)
        {
            std::cerr << "Error in switching socket to non-blocking mode socket_fd:" << fd;
            close(fd);
            exit(-1);
        }

        bzero((char *) &server_addr, sizeof(server_addr));

        server_addr.sin_family = AF_INET;
        server_addr.sin_addr.s_addr = INADDR_ANY;
        server_addr.sin_port = htons(port);

        if (bind(fd, (struct sockaddr *) &server_addr, sizeof(server_addr)) < 0) {
            std::cerr << "Error on binding socket_fd: " << fd << "on port: " << port << std::endl;
            close(fd);
            exit(1);
        }

        if (listen(fd, 32) < 0)
        {
            std::cerr << "Error in preparing to accept connections on socket_fd: " << fd << std::endl;
            close(fd);
            exit(1);
        }

        accept_fd = pollfd{fd, POLLIN, 0};
    }

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
