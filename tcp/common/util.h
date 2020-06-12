#ifndef UTIL_H_
#define UTIL_H_

#include <vector>
#include <unistd.h>
#include "chat_message.h"

class eof_exception : public std::exception {};

void read_n_bytes(int socket_fd, void* buffer, size_t n) {
    size_t bytes_read = 0;

    auto byte_buffer = reinterpret_cast<uint8_t *>(buffer);
    while (bytes_read < n) {
        ssize_t new_bytes = read(socket_fd, byte_buffer + bytes_read, n - bytes_read);
        if (new_bytes == 0) {
            throw eof_exception();
        }
        if (new_bytes < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        bytes_read += new_bytes;
    }
}

void write_n_bytes(int socket_fd, const void* buffer, size_t n) {
    size_t bytes_wrote = 0;

    auto byte_buffer = reinterpret_cast<const uint8_t *>(buffer);
    while (bytes_wrote < n) {
        ssize_t new_bytes = write(socket_fd, byte_buffer + bytes_wrote, n - bytes_wrote);
        if (new_bytes < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
        bytes_wrote += new_bytes;
    }
}

template <typename T>
T read_number(int socket_fd) {
    uint8_t buffer[sizeof(T)];

    read_n_bytes(socket_fd, buffer, sizeof(T));

    T result = 0;
    for (int i = 0; i < sizeof(T); i++) {
        result += T(buffer[i]) << (8 * i);
    }

    return result;
}

template <typename T>
void write_number(int socket_fd, T n) {
    uint8_t buffer[sizeof(T)];

    for (int i = 0; i < sizeof(T); i++) {
        buffer[i] = uint8_t(n >> (8 * i));
    }

    write_n_bytes(socket_fd, buffer, sizeof(T));
}

ChatMessage read_message(int socket_fd) {
    auto time = read_number<int64_t>(socket_fd);
    auto name_length = read_number<uint64_t>(socket_fd);
    std::vector<char> name(name_length + 1);
    read_n_bytes(socket_fd, name.data(), name_length);
    auto text_length = read_number<uint64_t>(socket_fd);
    std::vector<char> text(text_length + 1);
    read_n_bytes(socket_fd, text.data(), text_length);
    return {time, std::string(name.data()), std::string(text.data())};
}

void write_message(int socket_fd, const ChatMessage& message) {
    write_number<int64_t>(socket_fd, message.time);
    write_number<uint64_t>(socket_fd, message.name.length());
    write_n_bytes(socket_fd, message.name.c_str(), message.name.length());
    write_number<uint64_t>(socket_fd, message.text.length());
    write_n_bytes(socket_fd, message.text.c_str(), message.text.length());
}

#endif
