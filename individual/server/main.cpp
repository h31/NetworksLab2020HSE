#include <iostream>
#include <asm/ioctls.h>
#include <stropts.h>
#include "server.h"

int parse_port_arg(const std::string& arg) {
    int port;
    try {
        std::size_t pos;
        port = std::stoi(arg, &pos);
        if (pos < arg.size()) {
            throw std::invalid_argument("Trailing characters after number: " + arg + "\n");
        }
    } catch (std::invalid_argument const &ex) {
        throw std::invalid_argument("Invalid number: " + arg + "\n");
    } catch (std::out_of_range const &ex) {
        throw std::invalid_argument("Number out of range: " + arg + "\n");
    }
    return port;
}

int main(int argc, char *argv[]) {
    int port;

    if (argc < 2) {
        fprintf(stderr, "Usage %s port\n", argv[0]);
        exit(1);
    }

    try {
        port = parse_port_arg(argv[1]);
    } catch (std::invalid_argument const &ex) {
        fprintf(stderr, "%s", ex.what());
        exit(1);
    }

    server server(port);
    server.start();

    std::string command;
    while (getline(std::cin, command)) {
        if (command == "exit") {
            server.stop();
            break;
        }
    }

    return 0;
}
