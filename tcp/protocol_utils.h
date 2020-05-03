#pragma once
// #include <stdio.h>
// #include <stdlib.h>

// #include <netdb.h>
// #include <netinet/in.h>
#include <unistd.h>
// #include <errno.h>

#include <string.h>

const size_t HEADER_SIZE = 16;
const size_t BUFFER_SIZE = 256;

int send_msg(int socket, char *buffer) {
    int n = 0;
    char header[HEADER_SIZE];
    bzero(header, HEADER_SIZE);
    sprintf(header, "%zu", strlen(buffer));

    n = write(socket, header, HEADER_SIZE);
    if (n <= 0) {
        return n;
    }

    n = write(socket, buffer, strlen(buffer));
    return n;
}

int read_msg(int socket, char *buffer) {
    int n = 0;
    char header[HEADER_SIZE];
    bzero(header, HEADER_SIZE);
    // printf("\nreading head\n");
    n = read(socket, header, HEADER_SIZE);
    if (n <= 0) {
        return n;
    }
    // printf("\nreadding msg\n");
    size_t len = atoi(header);
    n = read(socket, buffer, len);
    return n;
}

