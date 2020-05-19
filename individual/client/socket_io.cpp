#include <netinet/in.h>
#include <chrono>
#include "socket_io.h"


static time_t millis_to_time(uint64_t millis) {
    auto duration = std::chrono::duration<uint64_t, std::milli>(millis);
    std::chrono::system_clock::time_point time_point(duration);
    return std::chrono::system_clock::to_time_t(time_point);
}

time_t socket_io::read_time(char* buffer) const {
    uint64_t millis = read_int64(buffer);
    if (millis < 0) {
        return -1;
    }
    return millis_to_time(millis);
}

int socket_io::write_time() const {
    uint64_t millis = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();
    return write_int64(millis);
}

int socket_io::write_int64(uint64_t value) const {
    char buffer[sizeof(uint64_t)];
    uint64_t nvalue = htobe64(value);
    memcpy(buffer, &nvalue, sizeof(uint64_t));
    return write_bytes(sizeof(uint64_t), buffer);
}

uint64_t socket_io::read_int64(char* buffer) const {
    if (read_bytes(sizeof(uint64_t), buffer) < 0) {
        return -1;
    }
    uint64_t value;
    memcpy(&value, buffer, sizeof(uint64_t));
    return be64toh(value);
}

uint32_t socket_io::read_int32(char* buffer) const {
    if (read_bytes(sizeof(uint32_t), buffer) < 0) {
        return -1;
    }
    uint32_t value;
    memcpy(&value, buffer, sizeof(uint32_t));
    return ntohl(value);
}

uint32_t socket_io::write_int32(uint32_t value) const {
    char buffer[sizeof(uint32_t)];
    uint32_t nvalue = htonl(value);
    memcpy(buffer, &nvalue, sizeof(uint32_t));
    return write_bytes(sizeof(uint32_t), buffer);
}

int socket_io::read_bytes(unsigned int total_bytes, char* buffer) const {
    int cur_bytes = 0;
    while (cur_bytes < total_bytes) {
        ssize_t nbytes = read(socket_fd, buffer, total_bytes - cur_bytes);
        if (nbytes < 0) {
            return -1;
        }
        buffer += nbytes;
        cur_bytes += nbytes;
    }
    return cur_bytes;
}

int socket_io::write_bytes(unsigned int total_bytes, const char* buffer) const {
    int cur_bytes = 0;
    while (cur_bytes < total_bytes) {
        ssize_t nbytes = write(socket_fd, buffer, total_bytes - cur_bytes);
        if (nbytes < 0) {
            return -1;
        }
        buffer += nbytes;
        cur_bytes += nbytes;
    }
    return cur_bytes;
}
