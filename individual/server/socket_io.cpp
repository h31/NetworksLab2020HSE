#include <netinet/in.h>
#include "socket_io.h"

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

uint32_t socket_io::read_bytes(uint32_t total_bytes, char* buffer) const {
    uint32_t cur_bytes = 0;
    while (cur_bytes < total_bytes) {
        uint32_t nbytes = read(socket_fd, buffer, total_bytes - cur_bytes);
        if (nbytes < 0) {
            return -1;
        }
        buffer += nbytes;
        cur_bytes += nbytes;
    }
    return cur_bytes;
}

uint32_t socket_io::write_bytes(uint32_t total_bytes, char* buffer) const {
    uint32_t cur_bytes = 0;
    while (cur_bytes < total_bytes) {
        uint32_t nbytes = write(socket_fd, buffer, total_bytes - cur_bytes);
        if (nbytes < 0) {
            return -1;
        }
        buffer += nbytes;
        cur_bytes += nbytes;
    }
    return cur_bytes;
}
