#include <netinet/in.h>

#include "Server.h"

int main(int argc, char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    uint32_t port = atoi(argv[1]);

    Server *server = nullptr;

    try {
        server = new Server(port);

        std::string command;
        while (true) {
            std::cin >> command;
            if (command == "quit") {
                break;
            }
        }

        delete server;
    } catch (const std::exception &e) {
        perror(e.what());
        delete server;
    }
}
