#ifndef SERIALIZATION_H
#define SERIALIZATION_H

#include <vector>
#include <unistd.h>

template<typename T>
std::vector<char> intToArray(T x) {
    int n = sizeof(x);
    std::vector<char> bytes(n);
    for (int i = 0; i < n; ++i) {
        bytes[n - 1 - i] = (x >> (i * 8)) & 0x000000ff;
    }
    return bytes;
}

template<typename T, std::size_t N>
T intFromArray(const char (& bytes)[N]) {
    T result = 0;
    for (std::size_t i = 0; i < N; ++i) {
        result |= uint8_t(bytes[N - 1 - i]) << (i * 8);
    }
    return result;
}

#endif
