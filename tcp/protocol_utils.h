#pragma once
#include <unistd.h>
#include <string.h>

#define HEADER_SIZE sizeof(size_t)
#define TEXT_SIZE 150
#define NAME_SIZE 20

struct message_t {
    time_t tm;
    char name[NAME_SIZE];
    char text[TEXT_SIZE];
};
typedef struct message_t message;

#define BUFFER_SIZE (sizeof(message) + 10)

int send_bytes(int socket, char *buffer, size_t size) {
    int n = 0;
    // char header[HEADER_SIZE];
    // bzero(header, HEADER_SIZE);
    // sprintf(header, "%zu", buf_len);
    // printf("header\n");
    // fflush(stdout);
    n = send(socket, &size, HEADER_SIZE, MSG_NOSIGNAL);
    // printf("send len\n");
    // fflush(stdout);
    if (n <= 0) {
        return n;
    }
    n = send(socket, buffer, size, MSG_NOSIGNAL);
    // printf("send data\n");
    // fflush(stdout);
    return n;
}

int read_bytes(int socket, char *buffer) {
    int n = 0;
    // char header[HEADER_SIZE];
    // bzero(header, HEADER_SIZE);
    // printf("\nreading head\n");
    size_t len = 0;
    n = read(socket, &len, HEADER_SIZE);
    if (n <= 0) {
        return n;
    }
    // printf("\nreadding msg\n");
    // printf("len");
    n = read(socket, buffer, len);
    return n;
}

void deserialize_msg(message *msg, char *buffer, size_t len) {
    bzero(msg->text, TEXT_SIZE);
    bzero(msg->name, NAME_SIZE);
    size_t shift = 0;
    memcpy(&msg->tm, buffer, sizeof(size_t));
    shift += sizeof(size_t);
    memcpy(msg->name, buffer + shift, NAME_SIZE);
    shift += NAME_SIZE;
    memcpy(msg->text, buffer + shift, len - shift);
}


size_t serialize_msg(const message *msg, char *buffer) {
    size_t shift = 0;
    memcpy(buffer, &msg->tm, sizeof(time_t));
    shift += sizeof(time_t);
    memcpy(buffer + shift, msg->name, NAME_SIZE);
    shift += NAME_SIZE;
    int len =  strlen(msg->text);
    memcpy(buffer + shift, msg->text, len + 1);
    shift += len + 1;
    return shift;
}