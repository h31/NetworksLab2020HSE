#include <cstdio>
#include <cstdlib>
#include "Client.h"

int main(int argc, char *argv[]) {
    if (argc != 4) {
        fprintf(stderr, "usage %s hostname port username\n", argv[0]);
        exit(0);
    }

    char *hostname = argv[1];
    char *port = argv[2];
    char *name = argv[3];

    new Client(hostname, port, name);
}