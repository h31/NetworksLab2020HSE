#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <pthread.h>

#include <string.h>

const uint32_t MSG_LEN_LEN = sizeof(uint32_t);
const uint32_t MSG_UTC_LEN = sizeof(int8_t);
const uint32_t MSG_TIME_LEN = sizeof(uint32_t);
const uint32_t MAX_MSG_LEN = 512;
const uint32_t NAME_LEN = 32;
const uint32_t MSG_LEN = MAX_MSG_LEN - NAME_LEN - 3;

typedef struct message
{
    uint32_t size;
    int8_t utc_delta;
    uint32_t time_s;
    char* message_text = NULL;

    message();
    message(uint32_t size, int8_t utc, uint32_t time, char* message);
    ~message();

    int read_from_socket(int socket);
    int send_to_socket(int socket);

    void to_string(int8_t current_utc, char* buffer);
    uint32_t len_int_string_format();

} message_t;

const uint32_t SIM = 60;
const uint32_t SIH = SIM * 60;
const uint32_t SID = SIH * 24;

int readn(int socket, char* buffer, int n);
int getmessage(int socket, char* buffer);
int getmessagelen(int socket);
int sendmessage(int socket, int len, char* buff);
