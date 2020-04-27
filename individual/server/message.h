//
// Created by maria on 27.04.2020.
//

#ifndef SERVER_MESSAGE_H
#define SERVER_MESSAGE_H


#include <cstdint>
#include <unistd.h>
#include <memory>
#include <cstring>
#include <netinet/in.h>

class message {
private:
    const uint32_t HEADER_SIZE = sizeof(uint32_t);
    uint32_t head;
    uint32_t tail;
    bool header;
    std::shared_ptr<char> data;

    void resize(uint32_t size) {
        std::shared_ptr<char> new_data(new char[HEADER_SIZE + size], std::default_delete<char[]>());
        memcpy(data.get(), new_data.get(), HEADER_SIZE);
        data = new_data;
        tail += size;
    }

    uint32_t parse_header() {
        uint32_t size;
        memcpy(&size, data.get(), HEADER_SIZE);
        return ntohl(size);
    }

public:
    message() : head(0), tail(HEADER_SIZE), header(true), data(new char[tail], std::default_delete<char[]>()) { };

    int32_t read(int fd) {
        int32_t nbytes = ::read(fd, data.get() + head, tail - head);
        if (nbytes < 0) {
            return -1;
        }
        head += nbytes;
        if (header && head == tail) {
            uint32_t size = parse_header();
            resize(size);
            header = false;
            std::cout << "Receive message header:" << size << " from client on socket:" << fd << std::endl;
        }
        return nbytes;
    }

    int32_t write(int fd) {
        int32_t nbytes = ::write(fd, data.get() + head, tail - head);
        if (nbytes < 0) {
            return -1;
        }
        head += nbytes;
        return nbytes;
    }

    bool full() const {
        return !header && head == tail;
    }

    void clear() {
        head = 0;
        tail = HEADER_SIZE;
        header = true;
        data = std::shared_ptr<char>(new char[tail], std::default_delete<char[]>());
    }

    void flip() {
        head = 0;
    }
};


#endif //SERVER_MESSAGE_H
