#include <string>

#include <netdb.h>
#include <cstring>
#include <iostream>
#include <ctime>
#include <iomanip>

#include "client.h"
#include "../common/chat_message.h"
#include "../common/util.h"

ChatClient::ChatClient(const std::string& user_name, const std::string& host_name, uint16_t port)
        : user_name(user_name), host_name(host_name), port(port) {}

void ChatClient::start() {
    int socket_fd = -1, error_no;

    addrinfo hints = {};
    addrinfo *addresses;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    error_no = getaddrinfo(host_name.c_str(), std::to_string(port).c_str(), &hints, &addresses);
    if (error_no != 0) {
        std::cerr << host_name << ": " << strerror(error_no) << std::endl;
        exit(1);
    }

    for (addrinfo *address = addresses; address != nullptr; address = address->ai_next) {
        socket_fd = socket(address->ai_family, address->ai_socktype, address->ai_protocol);
        if (socket_fd == -1) {
            error_no = errno;
            break;
        }

        if (connect(socket_fd, address->ai_addr, address->ai_addrlen) == 0) {
            break;
        }

        error_no = errno;
        close(socket_fd);
        socket_fd = -1;
    }

    freeaddrinfo(addresses);

    if (socket_fd == -1) {
        std::cerr << host_name << ": " << strerror(error_no) << std::endl;
        exit(1);
    }

    this->socket_fd = socket_fd;

    input_thread = std::thread([this]{ input_loop(); });
    output_thread = std::thread([this]{ output_loop(); });
}

void ChatClient::wait() {
    output_thread.join();
    input_thread.join();
    close(socket_fd);

}

void ChatClient::input_loop() {
    try {
        while (true) {
            auto message = read_message(socket_fd);
            auto &local_time = *std::localtime(&message.time);
            std::cout
                    << "<" << std::setfill('0') << std::setw(2) << local_time.tm_hour
                    << ":" << std::setfill('0') << std::setw(2) << local_time.tm_min << "> "
                    << "[" << message.name << "] " << message.text << std::endl;
        }
    } catch (const eof_exception&) {
        bool stopped_snapshot;
        {
            std::lock_guard<std::mutex> lock(stopped_mutex);
            stopped_snapshot = stopped;
        }
        if (!stopped_snapshot) {
            std::cout << "Server stopped. Press ENTER to exit." << std::endl;
            close_loops();
        }
    }
}

void ChatClient::output_loop() {
    while (true) {
        std::string text;
        std::getline(std::cin, text);
        if (text == "exit") {
            std::lock_guard<std::mutex> lock(stopped_mutex);
            stopped = true;
        }
        {
            std::lock_guard<std::mutex> lock(stopped_mutex);
            if (stopped) {
                break;
            }
            auto time = std::time(nullptr);
            ChatMessage message{time, user_name, text};
            write_message(socket_fd, message);
        }
    }
    close_loops();
}

void ChatClient::close_loops() {
    std::lock_guard<std::mutex> lock(stopped_mutex);
    stopped = true;
    shutdown(socket_fd, SHUT_WR);
}
