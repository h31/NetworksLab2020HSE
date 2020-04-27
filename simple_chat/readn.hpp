#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <fcntl.h>
#include <poll.h>

#include <pthread.h>

#include <string.h>

const uint32_t MSG_LEN_LEN = sizeof(int);
const uint32_t MAX_MSG_LEN = 512;
const uint32_t NAME_LEN = 32;
const uint32_t MSG_LEN = MAX_MSG_LEN - NAME_LEN - 3;
const uint32_t TEXT_MSG_LEN = NAME_LEN + 3 + NAME_LEN;

const int TIMEOUT = 10000;

int readn(int socket, char* buffer, int n);
int getmessage(int socket, char* buffer);
int sendmessage(int socket, int len, char* buff);

typedef struct pollfd pollfd_t;

typedef struct client_info {
    int socket;
    int message_read_len;
    int read_len;
    char* read_buffer;
    int message_write_len;
    int write_len;
    char* write_buffer;

    client_info(int sockfd);
    ~client_info();

    int set_write(char* buffer, int message_len);

    void set_read();

} client_info_t;

int read_from(client_info_t* info);
int write_to(client_info_t* info);
