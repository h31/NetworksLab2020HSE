#include <boost/asio/post.hpp>
#include <iostream>
#include "client.h"
#include "chat.h"

void client::loop() {
    fprintf(stdout, "Client has started with socket: %d", socket_fd);
    std::cout << std::endl;
    while (running) {
        unsigned int prefix_size = 4;
        char prefix[prefix_size];
        unsigned int body_size = io.read_int(prefix);

        if (body_size < 0) {
            chat_ptr->remove_client(this);
            return;
        }

        message msg(body_size, prefix, prefix_size);
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
        if (!running) {
            return;
        }
        if (io.write_bytes(msg.get_msg_size(), msg.get_prefix_ptr()) < 0) {
            shutdown();
        }
    });
}
