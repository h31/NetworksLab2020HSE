#ifndef SERVER_SOCKET_IO_H
#define SERVER_SOCKET_IO_H

#include <unistd.h>
#include <cstring>

class socket_io {
private:
    int socket_fd;

public:
    socket_io() {}

    socket_io(int socket_fd) : socket_fd(socket_fd) {}

    int32_t read_bytes(unsigned int total_bytes, char* buffer) const;

    int32_t read_int32(char* buffer) const;

    int32_t write_int32(uint32_t value) const;

    int write_bytes(unsigned int total_bytes, const char* buffer) const;

    time_t read_time(char* buffer) const;

    int write_int64(uint64_t value) const;

    int64_t read_int64(char* buffer) const;

    int write_time() const;
};


#endif //SERVER_SOCKET_IO_H
