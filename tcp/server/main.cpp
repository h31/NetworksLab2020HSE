#include "Server.h"

int main(int argc, char* argv[]) {
    if (argc != 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    uint16_t port = atoi(argv[1]);

    Server server(port);
    server.run();

    return 0;
}
