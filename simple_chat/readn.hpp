#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <pthread.h>

#include <string.h>

const uint32_t MSG_LEN_LEN = sizeof(int);
const uint32_t MAX_MSG_LEN = 512;
const uint32_t NAME_LEN = 32;
const uint32_t MSG_LEN = MAX_MSG_LEN - NAME_LEN - 3;

int readn(int socket, char* buffer, int n);
int getmessage(int socket, char* buffer);
int sendmessage(int socket, int len, char* buff);
