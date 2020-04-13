#ifndef IOUTILS_H
#define IOUTILS_H

#include <cstddef>
#include <unistd.h>

int writeFullBuffer(int socketFD, const char* buffer, std::size_t length) {
    unsigned int i = 0;
    while (i < length) {
        auto written = write(socketFD, buffer + i, length - i);
        if (written < 0) {
            return -1;
        }
        i += written;
    }
    return 0;
}

int readFullBuffer(int socketFD, char* buffer, std::size_t length) {
    unsigned int i = 0;
    while (i < length) {
        auto x = read(socketFD, buffer + i, length - i);
        if (x < 0) return -1;
        i += x;
    }
    return 0;
}

#endif
