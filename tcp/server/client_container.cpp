#include "client_container.h"

void ClientContainer::add_client(int socket_fd) {
    std::lock_guard<std::mutex> lock(mutex);
    client_loops.emplace_back(socket_fd, *this);
    client_loops.back().start();
}

void ClientContainer::notify_all_clients(std::shared_ptr<ChatMessage> message) {
    std::lock_guard<std::mutex> lock(mutex);
    for (auto& client : client_loops) {
        client.send(message);
    }
}

void ClientContainer::stop() {
    std::lock_guard<std::mutex> lock(mutex);
    for (auto& client : client_loops) {
        client.stop();
    }
    client_loops.clear();
}
