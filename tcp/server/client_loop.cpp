#include "client_loop.h"

#include <iostream>
#include <sys/socket.h>

#include "client_container.h"
#include "../common/util.h"

ClientLoop::ClientLoop(int socket_fd, ClientContainer& container) :
        socket_fd(socket_fd), container(container) {
}

void ClientLoop::start() {
    input_thread = std::thread([this]{ input_loop(); });
    output_thread = std::thread([this]{ output_loop(); });
}

void ClientLoop::stop() {
    stop_loops();
    input_thread.join();
    output_thread.join();
    close(socket_fd);
}

void ClientLoop::send(std::shared_ptr<ChatMessage> message) {
    queue.push(message);
}

void ClientLoop::input_loop() {
    try {
        while (true) {
            auto message = read_message(socket_fd);
            container.notify_all_clients(std::make_shared<ChatMessage>(message));
        }
    } catch (const eof_exception&) {
        stop_loops();
    }
}

void ClientLoop::output_loop() {
    try {
        while (true) {
            std::shared_ptr<ChatMessage> message = queue.pop();
            write_message(socket_fd, *message);
        }
    } catch (const queue_closed_exception&) {
    }
}

void ClientLoop::stop_loops() {
    queue.close();
    shutdown(socket_fd, SHUT_WR);
}
