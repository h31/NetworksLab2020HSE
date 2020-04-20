#include "readn.hpp"

int readn(int socket, char* buffer, int n) {
	int m = 0;
	while(m < n) {
        int cnt = read(socket, buffer + m, n);
		if (cnt <= 0) {
			return -1;
		}
		m += cnt;
	}
	return m;
}


int getmessage(int socket, char* buffer) {
    bzero(buffer, MAX_MSG_LEN);
    int len = 0;
    int n = readn(socket, (char*) &len, MSG_LEN_LEN);
    if (n < 0) {
        return -1;
    }
    return readn(socket, buffer, len);
}

int sendmessage(int socket, int len, char* buff) {
    char msg[MSG_LEN_LEN + len];
    bzero(msg, MSG_LEN_LEN + len);
    memcpy(msg, &len, MSG_LEN_LEN);
    memcpy(msg + MSG_LEN_LEN, buff, len);
    return write(socket, msg, MSG_LEN_LEN + len);
}