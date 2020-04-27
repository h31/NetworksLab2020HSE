#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <string.h>
#include <iostream>
#include "client.h"

int main(int argc, char *argv[]) {
    int socket_fd, err;

    if (argc < 4) {
        fprintf(stderr, "Usage %s name hostname port\n", argv[0]);
        exit(0);
    }

    char* name = argv[1];
    char* hostname = argv[2];
    char* port = argv[3];

    struct addrinfo hints = {}, *addrs;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    err = getaddrinfo(hostname, port, &hints, &addrs); // gethostbyname is deprecated
    if (err != 0)
    {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    for (struct addrinfo *addr = addrs; addr != NULL; addr = addr->ai_next)
    {
        socket_fd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (socket_fd == -1)
        {
            err = errno;
            break;
        }

        if (connect(socket_fd, addr->ai_addr, addr->ai_addrlen) == 0)
        {
            break;
        }

        err = errno;

        close(socket_fd);
        socket_fd = -1;
    }

    freeaddrinfo(addrs);

    if (socket_fd == -1)
    {
        fprintf(stderr, "%s: %s\n", hostname, strerror(err));
        exit(1);
    }

    client(name, socket_fd);

    return 0;
}
