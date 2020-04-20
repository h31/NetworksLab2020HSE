#ifndef SERVER_SOCKET_IO_H
#define SERVER_SOCKET_IO_H


#include <boost/shared_ptr.hpp>
#include <unistd.h>
#include <cstring>

class message {

private:
    uint32_t body_size;
    std::shared_ptr<char> body;

public:
    message(unsigned int body_size) : body_size(body_size) {
        body = std::shared_ptr<char>(new char[body_size], std::default_delete<char[]>());
    }

    char* get_body_ptr() const {
        return body.get();
    }

    uint32_t get_body_size() const {
        return body_size;
    }
};

class socket_io {
private:
    int socket_fd;

public:
    socket_io(int socket_fd) : socket_fd(socket_fd) {}

    uint32_t read_bytes(unsigned int total_bytes, char* buffer) const;

    uint32_t write_bytes(unsigned int total_bytes, char* buffer) const;

    uint32_t read_int32(char* buffer) const;

    uint32_t write_int32(uint32_t value) const;
};


#endif //SERVER_SOCKET_IO_H
