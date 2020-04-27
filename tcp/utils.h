#ifndef UTILS
#define UTILS

#include <netinet/in.h>

static uint16_t uint16_t_from_buffer(std::vector<uint8_t> &buffer) {
    uint16_t current = 0;
    for (uint8_t byte: buffer) {
        current = (current << 8u) | byte;
    }
    return ntohs(current);
}

static std::vector<uint8_t> uint16_t_to_buffer(uint16_t value) {
    std::vector<uint8_t> buffer;
    value = htons(value);
    uint8_t mask = 0 - 1;
    buffer.push_back((value >> 8u) & mask);
    buffer.push_back((value) & mask);
    return buffer;
}

static std::vector<uint8_t> get_text_message(const char* name, const char* text) {
    std::vector<uint8_t> buffer;
    buffer.push_back(strlen(name));
    buffer.insert(buffer.end(), name, name + strlen(name));
    std::vector<uint8_t> length_vec = uint16_t_to_buffer(strlen(text));
    buffer.insert(buffer.end(), length_vec.begin(), length_vec.end());
    buffer.insert(buffer.end(), text, text + strlen(text));
    return buffer;
}

static int write_buffer_to_socket(int sockfd, std::vector<uint8_t> buffer) {
    int left = buffer.size();
    while (left) {
        int current = write(sockfd, &buffer[buffer.size() - left], left);
        if (current < 0) {
            return current;
        } else {
            left -= current;
        }
    }
    return 0;
}

static int read_buffer_from_socket(int sockfd, std::vector<uint8_t> &buffer, size_t left) {
    while (left) {
        int current = read(sockfd, &buffer[buffer.size() - left], left);
        if (current < 0) {
            return current;
        }
        left -= current;
    }
    return 0;
}

static int read_full_message(int sockfd, std::vector<uint8_t> &name, std::vector<uint8_t> &text) {
    std::vector<uint8_t> name_length_vec(1), text_length_vec(2);

    int err = read_buffer_from_socket(sockfd, name_length_vec, 1);
    if (err < 0) {
        return err;
    }
    uint8_t name_length = name_length_vec[0];
    name.resize(name_length);
    err = read_buffer_from_socket(sockfd, name, name_length);
    if (err < 0) {
        return err;
    }

    err = read_buffer_from_socket(sockfd, text_length_vec, 2);
    if (err < 0) {
        return err;
    }
    uint16_t text_length = uint16_t_from_buffer(text_length_vec);
    text.resize(text_length);
    err = read_buffer_from_socket(sockfd, text, text_length);
    if (err < 0) {
        return err;
    }

    return 0;
}

#endif