#include <cstdio>
#include <iostream>
#include "server.h"

void server::loop() {
    fprintf(stdout, "Server has started on port: %d", port);
    std::cout << std::endl;
    while (running) {
        listen(socket_fd, 5);
        unsigned int addr_size = sizeof(sockaddr_in);
        sockaddr_in client_addr{};
        int client_socket_fd = accept(socket_fd, reinterpret_cast<sockaddr*>(&client_addr), &addr_size);
        if (client_socket_fd < 0) {
            fprintf(stderr, "Error on accepting client: %d\n", client_socket_fd);
            continue;
        }
        clients_chat.add_client(client_socket_fd);
    }
}

void server::shutdown() {
    if (running.exchange(false)) {
        fprintf(stdout, "Start shutdown server on port: %d", port);
        std::cout << std::endl;

        clients_chat.shutdown();

        ::shutdown(socket_fd, SHUT_RDWR);
        close(socket_fd);
        accept_thread.join();

        fprintf(stdout, "Finish shutdown server on port: %d", port);
        std::cout << std::endl;
    }
}

int server::connect_client() const {

}
