#pragma once
#include <unistd.h>
#include <string.h>
#include <stdint.h>
#include <errno.h>

#define HEADER_SIZE sizeof(int32_t)
#define TEXT_SIZE 512
#define NAME_SIZE 32

struct message_t {
    time_t tm;
    char name[NAME_SIZE];
    char text[TEXT_SIZE];
};
typedef struct message_t message;

#define BUFFER_SIZE (sizeof(message) + 10)

struct IncompliteBuffer_t {
    int32_t target_size;
    int32_t actual_size;
    char buffer[BUFFER_SIZE];
};
typedef struct IncompliteBuffer_t IncompliteBuffer;

void clear_buffer(IncompliteBuffer *iBuffer) {
    iBuffer->target_size = -1;
    iBuffer->actual_size = 0;
    bzero(iBuffer->buffer, BUFFER_SIZE);
}

int send_bytes(int socket, const IncompliteBuffer *iBuffer, int32_t offset) {
    int n = 0;

    n = send(socket, &iBuffer->actual_size, HEADER_SIZE, MSG_NOSIGNAL);
    
    if (n <= 0) {
        return n;
    }

    n = send(socket, iBuffer->buffer + offset, iBuffer->actual_size - offset, MSG_NOSIGNAL);
    return n;
}

int blocking_send_bytes(int socket, const IncompliteBuffer *iBuffer) {
    int32_t i = 0;
    while (i < iBuffer->actual_size) {
        int n = send_bytes(socket, iBuffer, i);
        if (n <= 0) {
            return n;
        }
        i += n;
    }
    return i;
}

int read_bytes(int socket, IncompliteBuffer *iBuffer) {
    int n = 0;

    if (iBuffer->target_size == -1) {
        n = read(socket, &iBuffer->target_size, HEADER_SIZE);
        if (n <= 0) {
            return n;
        }
        iBuffer->actual_size  = 0;
    }

    n = read(socket, iBuffer->buffer + iBuffer->actual_size, iBuffer->target_size - iBuffer->actual_size);
    iBuffer->actual_size += n;
    return n;
}

int blocking_read_bytes(int socket, IncompliteBuffer *iBuffer) {
    clear_buffer(iBuffer);
    while (iBuffer->actual_size != iBuffer->target_size) {
        int n = read_bytes(socket, iBuffer);
        if (n <= 0) {
            return n;
        }
    }
    return iBuffer->actual_size;
}


void deserialize_msg(message *msg, const char *buffer, uint32_t len) {
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

void deserialize_msg_from_iBuffer(message *msg, const IncompliteBuffer *iBuffer) {
    deserialize_msg(msg, iBuffer->buffer, iBuffer->actual_size);
}

uint32_t serialize_msg_to_iBuffer(const message *msg, IncompliteBuffer *iBuffer) {
    iBuffer->actual_size = iBuffer->target_size = serialize_msg(msg, iBuffer->buffer);
    return iBuffer->actual_size;
}