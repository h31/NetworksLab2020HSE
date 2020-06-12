#include <netdb.h>
#include <zconf.h>

#include "accept_loop.h"

void AcceptLoop::start(int socket_fd) {
    this->socket_fd = socket_fd;
    accept_thread = std::thread([this]{ loop(); });
}

void AcceptLoop::stop() {
    shutdown(socket_fd, SHUT_RDWR);
    close(socket_fd);
    accept_thread.join();
}

void AcceptLoop::loop() {
    sockaddr_in client_address{};
    unsigned int client_address_len = sizeof(client_address);

    while (true) {
        int client_socket_fd = accept(socket_fd, (sockaddr *) &client_address, &client_address_len);
        if (client_socket_fd < 0) {
            break;
        }
        container.add_client(client_socket_fd);
    }
    container.stop();
}
