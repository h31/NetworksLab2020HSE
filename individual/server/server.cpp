#include <iostream>
#include <memory>
#include <asm/ioctls.h>
#include <stropts.h>
#include "server.h"

void server::loop() {
    std::cout << "Start server on socket:" << accept_fd.fd << " port:" << port << std::endl;

    while (running) {

        int accept_fd_index = clients.size();
        pollfd fds[accept_fd_index + 1];
        int i = 0;
        for (const auto& client : clients)
        {
            fds[i++] = client.second->get_fd();
        }
        fds[accept_fd_index] = accept_fd;

        if (poll(fds, accept_fd_index + 1, 3500) < 0) {
            std::cerr << "Error on polling sockets" << std::endl;
            break;
        }

        accept_clients();

        for (int i = 0; i < accept_fd_index; i++)
        {
            if (fds[i].revents & POLLIN) {
                read(fds[i].fd);
            }
            if (fds[i].revents & POLLOUT) {
                write(fds[i].fd);
            }
        }
    }
}


void server::accept_clients() {
    while (running) {
        int fd = accept(accept_fd.fd, nullptr, nullptr);
        if (fd < 0) {
            if (running && errno != EWOULDBLOCK) {
                std::cerr << "Error on accepting client" << std::endl;
            }
            break;
        }
        if (ioctl(fd, FIONBIO, (char*) &ON) < 0) {
            std::cerr << "Error on switching client socket" << fd << "to non-blocking mode" << std::endl;
            close(fd);
        }
        clients[fd] = std::make_shared<client>(fd, this);
    }
}

void server::remove_client(int fd) {
    if (clients.find(fd) != clients.end()) {
        clients.erase(fd);
    }
}

void server::read(int fd) {
    if (clients.find(fd) != clients.end()) {
        auto client = clients[fd].get();
        if (client->read() < 0) {
            remove_client(fd);
        }
        if (client->ready()) {
            write_all(client->get_msg());
        }
    }
}

void server::write(int fd) {
    if (clients.find(fd) != clients.end()) {
        auto client = clients[fd].get();
        if (client->write() < 0) {
            remove_client(fd);
        }
    }
}

void server::write_all(const message& msg) {
    for (const auto& client : clients) {
        client.second->put_msg(msg);
    }
}

void server::stop() {
    if (running.exchange(false)) {
        std::cout << "Shutdown server on socket:" << accept_fd.fd << " port:" <<  port << std::endl;
        clients.clear();
        close(accept_fd.fd);
        loop_thread.join();
    }
}

void server::start() {
    if (running.exchange(true)) {
        std::cout << "Server has already started on socket:" << accept_fd.fd << " port:" <<  port << std::endl;
        return;
    }
    loop_thread = std::thread(&server::loop, this);
}
