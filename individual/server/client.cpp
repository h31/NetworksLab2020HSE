#include <iostream>
#include "client.h"

int client::read() {
    int nread = input.read(fd.fd);

    if (nread > 0) {
        std::cout << "Read " << nread << " bytes from client on socket:" << fd.fd << std::endl;
    }
    return nread;
}

bool client::ready() {
    return input.full();
}

void client::put_msg(const message& msg) {
    std::cout << "Add new message to client on socket:" << fd.fd << std::endl;

    fd.events = POLLIN | POLLOUT;
    output.push(msg);
}

message client::get_msg() {
    std::cout << "Extract message from client on socket:" << fd.fd << std::endl;

    message msg = input;
    msg.flip();
    input.clear();
    return msg;
}

int client::write() {
    std::cout << "Write message to client on socket:" << fd.fd << std::endl;

    if (output.empty()) {
        fd.events = POLLIN;
        return 0;
    }
    int nwrite = output.front().write(fd.fd);
    if (output.front().full()) {
        output.pop();
        if (output.empty()) {
            fd.events = POLLIN;
        }
    }
    return nwrite;
}
