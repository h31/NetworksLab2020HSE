#include <netinet/in.h>
#include "socket_io.h"

uint32_t socket_io::read_int(char* buffer) const {
    if (read_bytes(sizeof(uint32_t), buffer) < 0) {
        return -1;
    }
    uint32_t value;
    memcpy(&value, buffer, sizeof(uint32_t));
    return ntohl(value);
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

int socket_io::write_bytes(unsigned int total_bytes, char* buffer) const {
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
