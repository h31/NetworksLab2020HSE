#pragma once
#include <unistd.h>
#include <string.h>
#include <stdint.h>

#define HEADER_SIZE sizeof(uint32_t)
#define TEXT_SIZE 512
#define NAME_SIZE 32

struct message_t {
    time_t tm;
    char name[NAME_SIZE];
    char text[TEXT_SIZE];
};
typedef struct message_t message;

#define BUFFER_SIZE (sizeof(message) + 10)

int send_bytes(int socket, char *buffer, uint32_t size) {
    int n = 0;

    n = send(socket, &size, HEADER_SIZE, MSG_NOSIGNAL);
    
    if (n <= 0) {
        return n;
    }
    n = send(socket, buffer, size, MSG_NOSIGNAL);
    
    return n;
}

int read_bytes(int socket, char *buffer) {
    int n = 0;

    uint32_t len = 0;
    n = read(socket, &len, HEADER_SIZE);
    if (n <= 0) {
        return n;
    }

    n = read(socket, buffer, len);
    return n;
}

void deserialize_msg(message *msg, char *buffer, uint32_t len) {
    bzero(msg->text, TEXT_SIZE);
    bzero(msg->name, NAME_SIZE);
    uint32_t shift = 0;
    memcpy(&msg->tm, buffer, sizeof(time_t));
    shift += sizeof(time_t);
    memcpy(msg->name, buffer + shift, NAME_SIZE);
    shift += NAME_SIZE;
    memcpy(msg->text, buffer + shift, len - shift);
}


uint32_t serialize_msg(const message *msg, char *buffer) {
    uint32_t shift = 0;
    memcpy(buffer, &msg->tm, sizeof(time_t));
    shift += sizeof(time_t);
    memcpy(buffer + shift, msg->name, NAME_SIZE);
    shift += NAME_SIZE;
    int len =  strlen(msg->text);
    memcpy(buffer + shift, msg->text, len + 1);
    shift += len + 1;
    return shift;
}