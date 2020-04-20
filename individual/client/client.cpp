#include <iostream>
#include "client.h"

void client::read_loop() {
    char body_size_buffer[sizeof(uint32_t)];
    char time_buffer[sizeof(uint64_t)];
    char name_size_buffer[sizeof(uint32_t)];
    char msg_size_buffer[sizeof(uint32_t)];
    while (true) {
        if (io.read_int32(body_size_buffer) < 0) {
            exit(1);
        }

        time_t time = io.read_time(time_buffer);
        if (time < 0) {
            exit(1);
        }

        int name_size = io.read_int32(name_size_buffer);
        if (name_size < 0) {
            exit(1);
        }

        char* name_buffer = new char[name_size];
        if (io.read_bytes(name_size, name_buffer) < 0) {
            exit(1);
        }

        int msg_size = io.read_int32(msg_size_buffer);
        if (msg_size < 0) {
            exit(1);
        }

        char* msg_buffer = new char[msg_size];
        if (io.read_bytes(msg_size, msg_buffer) < 0) {
            exit(1);
        }
        std::string time_str = std::ctime(&time);
        printf("<%s> [%s] %s\n", time_str.substr( 0, time_str.length() -1  ).c_str(), name_buffer, msg_buffer);
    }
}

void client::write_loop() {
    std::string msg;
    while (true) {
        std::getline(std::cin, msg);
        uint32_t name_size = name.length();
        uint32_t msg_size = msg.length();
        uint32_t body_size = 8 + 4 + name_size + 4 + msg_size;
        if (io.write_int32(body_size) < 0 || io.write_time() < 0 ||
                io.write_int32(name_size) < 0 || io.write_bytes(name_size, name.c_str()) < 0 ||
                io.write_int32(msg_size) < 0 || io.write_bytes(msg_size, msg.c_str()) < 0)
        {
            exit(1);
        }
    }
}

void client::shutdown() {

}
