#include <stdio.h>
#include <stdlib.h>

#include <netinet/in.h>

#include "server.h"

int main(int argc, char *argv[]) {
    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    uint16_t portno = (uint16_t) std::stoi(argv[1]);
    Server srv(portno);
    srv.Serve();

    return 0;
}
