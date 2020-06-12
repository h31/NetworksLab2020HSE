#include <cstdio>
#include <iostream>

#include "client.h"

int main(int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "usage: " << argv[0] << " username hostname port" << std::endl;
        exit(0);
    }

    char* username = argv[1];
    char* hostname = argv[2];
    char* port = argv[3];

    ChatClient client(username, hostname, static_cast<uint16_t>(std::stoi(port)));
    client.start();
    client.wait();

    return 0;
}
