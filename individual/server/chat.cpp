#include <boost/asio/post.hpp>
#include <iostream>
#include "chat.h"

inline bool operator==(std::shared_ptr<client> const& a, client* const b) {
    return *a == *b;
}

void chat::remove_client(client* client) {
    if (running) {
        boost::asio::post(chat_thread, [=]() {
            client->shutdown();
            mtx.lock();
            auto client_ptr = std::find(clients.begin(), clients.end(), client);
            if (client_ptr != clients.end()) {
                clients.erase(client_ptr);
            }
            mtx.unlock();
        });
    }
}

void chat::add_client(int socket_fd) {
    if (running) {
        mtx.lock();
        clients.push_back(std::make_shared<client>(socket_fd, this));
        mtx.unlock();
    }
}

void chat::send(const message& msg) {
    if (running) {
        mtx.lock();
        for (const auto &client : clients) {
            client->send(msg);
        }
        mtx.unlock();
    }
}

void chat::shutdown() {
    if (running.exchange(false)) {
        fprintf(stdout, "Start shutdown chat");
        std::cout << std::endl;

        chat_thread.join();
        mtx.lock();
        for (const auto& client : clients) {
            client->shutdown();
        }
        mtx.unlock();

        fprintf(stdout, "Finish shutdown chat");
        std::cout << std::endl;
    }
}
