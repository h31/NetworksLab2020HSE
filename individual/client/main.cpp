#include <stdlib.h>
#include <iostream>
#include "client.h"

int main(int argc, char *argv[]) {
    if (argc < 4) {
        std::cerr << "Usage "<< argv[0] << " name hostname port" << std::endl;
        exit(0);
    }

    char* name = argv[1];
    char* hostname = argv[2];
    char* port = argv[3];

    client client(name, hostname, port);
    client.start();

    return 0;
}
