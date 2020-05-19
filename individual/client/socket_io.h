#ifndef SERVER_SOCKET_IO_H
#define SERVER_SOCKET_IO_H

#include <unistd.h>
#include <cstring>

//class message {
//
//private:
//
//    unsigned int prefix_size;
//    unsigned int body_size;
//    std::shared_ptr<char> msg;
//
//public:
//    message(unsigned int body_size, char* prefix, unsigned int prefix_size = 4) : prefix_size(prefix_size), body_size(body_size) {
//        msg = std::shared_ptr<char>(new char[prefix_size + body_size], std::default_delete<char[]>());
//        memcpy(prefix, get_prefix_ptr(), prefix_size);
//    }
//
//    char* get_prefix_ptr() const {
//        return msg.get();
//    }
//
//    char* get_body_ptr() {
//        return msg.get() + prefix_size;
//    }
//
//    unsigned int get_prefix_size() {
//        return prefix_size;
//    }
//
//    unsigned int get_body_size() {
//        return prefix_size;
//    }
//
//    unsigned int get_msg_size() const {
//        return prefix_size + body_size;
//    }
//};

class socket_io {
private:
    int socket_fd;

public:
    socket_io(int socket_fd) : socket_fd(socket_fd) {}

    int read_bytes(unsigned int total_bytes, char* buffer) const;

    uint32_t read_int32(char* buffer) const;

    uint32_t write_int32(uint32_t value) const;

    int write_bytes(unsigned int total_bytes, const char* buffer) const;

    time_t read_time(char* buffer) const;

    int write_int64(uint64_t value) const;

    uint64_t read_int64(char* buffer) const;

    int write_time() const;
};


#endif //SERVER_SOCKET_IO_H
