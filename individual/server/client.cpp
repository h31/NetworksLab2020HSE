#include <boost/asio/post.hpp>
#include <iostream>
#include "client.h"
#include "chat.h"

void client::loop() {
    fprintf(stdout, "Client has started with socket: %d", socket_fd);
    std::cout << std::endl;
    while (running) {
        uint32_t prefix_size = sizeof(uint32_t);
        char prefix[prefix_size];
        int32_t body_size = io.read_int32(prefix);

        if (body_size < 0) {
            chat_ptr->remove_client(this);
            return;
        }

        message msg(body_size);
        if (io.read_bytes(body_size, msg.get_body_ptr()) < 0) {
            chat_ptr->remove_client(this);
            return;
        }
        chat_ptr->send(msg);
    }
}

void client::shutdown() {
    if (running.exchange(false)) {

        fprintf(stdout, "Start shutdown client with socket: %d", socket_fd);
        std::cout << std::endl;

        ::shutdown(socket_fd, SHUT_RDWR);
        close(socket_fd);
        read_thread.join();
        write_thread.join();

        fprintf(stdout, "Finish shutdown client with socket: %d", socket_fd);
        std::cout << std::endl;
    }
}

void client::send(const message& msg) {
    boost::asio::post(write_thread,[=]() {
        if (io.write_int32(msg.get_body_size()) < 0 || io.write_bytes(msg.get_body_size(), msg.get_body_ptr()) < 0) {
            chat_ptr->remove_client(this);
            return;
        }
    });
}
