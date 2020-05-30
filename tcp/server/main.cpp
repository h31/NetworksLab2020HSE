#include <iostream>
#include "Server.h"

int main(int argc, char* argv[]) {
    if (argc != 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    uint16_t port = std::stoi(argv[1]);

    Server server(port);

    while (true) {
        std::string command;
        std::cin >> command;
        if (command == "exit") {
            break;
        }
    }

    return 0;
}
