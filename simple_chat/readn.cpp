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


client_info::client_info(int sockfd): socket(sockfd) {
    message_write_len = write_len = 0;
    write_buffer = (char*) calloc(MSG_LEN, sizeof(char));
    message_read_len = read_len = 0;
    read_buffer = (char*) calloc(MSG_LEN, sizeof(char));
}

client_info::~client_info() {
    free(read_buffer);
    free(write_buffer);
}

int client_info::set_write(char* buffer, int message_len) {
    if (write_len != message_write_len) {
        return -1;
    } 
    write_len = 0;
    message_write_len = message_len;
    bzero(write_buffer, MSG_LEN);
    memcpy(write_buffer, buffer, message_len);
    return 0;
}

void client_info::set_read() {
    message_read_len = read_len = 0;
    bzero(read_buffer, MSG_LEN);
}

int read_from(client_info_t* info) {
    int n;
    if (info->message_read_len == 0) {
        n = read(info->socket, &(info->message_read_len), MSG_LEN_LEN);
        if (n < 0) {
            return -1;
        }
        if (n == 0) {
            return 0;
        }
    }

    if (info->message_read_len != info->read_len) {
        n = read(info->socket,
                info->read_buffer + info->read_len,
                info->message_read_len - info->read_len);
        if (n < 0) {
            return -1;
        }
        info->read_len += n;
    }

    if (info->read_len == info->message_read_len && info->message_read_len != 0) {
        return 1;
    }
    return 0;
}

int write_to(client_info_t* info) {
    if (info->message_write_len == 0) {
        return 0;
    }

    int n;
    if (info->message_write_len != info->write_len) {
        n = write(info->socket,
                info->write_buffer + info->write_len,
                info->message_write_len - info->write_len);
        if (n < 0) {
            return -1;
        }
        info->write_len += n;
    }

    if (info->write_len == info->message_write_len) {
        bzero(info->write_buffer, MSG_LEN);
        info->write_len = info->message_write_len = 0;
    }
    return 0;
}