#include "readn.hpp"

int readn(int socket, char* buffer, int n) {
    int m = 0;
    while(m < n) {
        int cnt = read(socket, buffer + m, n);
        if (cnt < 0) {
            return -1;
        } 
        if (cnt == 0) {
            break;
        }
        m += cnt;
    }
    return m;
}

message_t::message() {}
message_t::message(uint32_t size, int8_t utc, uint32_t time, char* message) : size(size), utc_delta(utc), time_s(time) {
    message_text = (char*) malloc(size * sizeof(char));
    memcpy(message_text, message, size);
}
message_t::~message() {
    if (message_text) {
        free(message_text);
    }
}

int message_t::read_from_socket(int socket) {
    int n = readn(socket, (char*) &size, MSG_LEN_LEN);
    if (n != MSG_LEN_LEN) {
        return -1;
    }
    n = readn(socket, (char*) &utc_delta, MSG_UTC_LEN);
    if (n != MSG_UTC_LEN) {
        return -1;
    }
    n = readn(socket, (char*) &time_s, MSG_TIME_LEN);
    if (n != MSG_TIME_LEN) {
        return -1;
    }
    message_text = (char*) malloc(size * sizeof(char));
    n = readn(socket, message_text, size);
    if (n != size) {
        return -1;
    }
    return 0;
}

int message_t::send_to_socket(int socket) {
    int n = write(socket, (char*) &size, MSG_LEN_LEN);
    if (n != MSG_LEN_LEN) {
        return -1;
    }
    
    n = write(socket, (char*) &utc_delta, MSG_UTC_LEN);
    if (n != MSG_UTC_LEN) {
        return -1;
    }

    n = write(socket, (char*) &time_s, MSG_TIME_LEN);
    if (n != MSG_TIME_LEN) {
        return -1;
    }

    n = write(socket, message_text, size);
    if (n != size) {
        return -1;
    }
    return 0;
}

void message_t::to_string(int8_t current_utc, char* buffer) {
    int delta_utc = current_utc - utc_delta;
    uint32_t time = (time_s + delta_utc * SIH + SID) % SID; 
    int h = time / SIH, m = time % SIH / SIM, s = time % SIM;
    sprintf(buffer, "<%i:%i:%i> %s", h, m, s, message_text);
}

uint32_t message_t::len_int_string_format() {
    return size + 11;
}