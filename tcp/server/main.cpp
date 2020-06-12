#include <cstdio>
#include <cstdlib>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <cstring>
#include <iostream>
#include "server.h"

int main(int argc, char *argv[]) {
    if (argc < 2) {
        std::cerr << "usage: " << argv[0] << " port" << std::endl;
        exit(1);
    }

    auto port = (uint16_t) std::stoi(argv[1]);

    ChatServer server(port);

    server.start();

    std::string input;
    while (true) {
        std::cout << "Write exit to stop server." << std::endl;
        std::getline(std::cin, input);
        if (input == "exit") {
            break;
        }
    }

    server.stop();

    return 0;
}
