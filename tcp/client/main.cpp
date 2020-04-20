#include <stdio.h>
#include <stdlib.h>

#include "client.h"

int main(int argc, char *argv[]) {
    if (argc < 3) {
        fprintf(stderr, "usage %s hostname port username\n", argv[0]);
        exit(0);
    }

    char* hostname = argv[1];
    char* port = argv[2];
    char* username = argv[3];
    Client client(hostname, port, username);
    client.Run();

    return 0;
}
