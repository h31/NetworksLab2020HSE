#include "../readn.hpp"

void send(char* name, char* message, client_info_t* info) {
    char buffer[TEXT_MSG_LEN];

    bzero(buffer, TEXT_MSG_LEN);

    strcpy(buffer, name);
    strcat(buffer, " : ");
    strcat(buffer, message);

    int len = strlen(buffer);
    
    char msg[MSG_LEN_LEN + len];
    bzero(msg, MSG_LEN_LEN + len);
    memcpy(msg, &len, MSG_LEN_LEN);
    memcpy(msg + MSG_LEN_LEN, buffer, len);

    info->set_write(msg, MSG_LEN_LEN + len);
}

void read_name(char* name) {
    printf("Enter your name: ");
    bzero(name, NAME_LEN);
    fgets(name, NAME_LEN - 1, stdin);
    
    for (int i = 0; name[i]; i++) {
        if (name[i] == '\n') {
            name[i] = 0;
            break;
        }
    }
}

void print_message(client_info_t* info) {
    time_t rawtime;
    time(&rawtime);
    struct tm* timeinfo = localtime(&rawtime);

    printf("<%i:%i:%i> %s", timeinfo->tm_hour, timeinfo->tm_min, timeinfo->tm_sec, info->read_buffer);
}

int main(int argc, char *argv[]) {
    int sockfd, err;

    if (argc < 3) {
        fprintf(stderr, "usage %s hostname port\n", argv[0]);
        exit(0);
    }

    char* hostname = argv[1];
    char* port = argv[2];

    struct addrinfo hints = {}, *addrs;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    err = getaddrinfo(hostname, port, &hints, &addrs);
    if (err != 0) {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    for(struct addrinfo *addr = addrs; addr != NULL; addr = addr->ai_next) {
        sockfd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (sockfd == -1) {
            err = errno;
            break;
        }

        if (connect(sockfd, addr->ai_addr, addr->ai_addrlen) == 0) {
            break;
        }

        err = errno;

        close(sockfd);
        sockfd = -1;
    }

    freeaddrinfo(addrs);

    if (sockfd == -1)
    {
        fprintf(stderr, "%s: %s\n", hostname, strerror(err));
        exit(1);
    }

    int ret = fcntl(sockfd, F_SETFL, O_NONBLOCK);
    if (ret < 0) {
        perror("non blocking set error");
        exit(1);   
    }

    pollfd_t fds[2];
    fds[0].fd = sockfd;
    fds[0].events = POLLIN | POLLOUT;

    fds[1].fd = STDIN_FILENO;
    fds[1].events = POLLIN;

    client_info_t info(sockfd);

    char name[NAME_LEN];
    read_name(name);

    char message[MSG_LEN];
    while (1) {
        int ret = poll(fds, 2, TIMEOUT);
        if (ret < 0) {
            perror("poll ERROR");
            close(sockfd);
            return 0;
        } else if (ret == 0) {
            continue;
        }

        if (fds[1].revents > 0) {
            bzero(message, MSG_LEN);
            fgets(message, MSG_LEN - 1, stdin);
            send(name, message, &info);
        }

        if (fds[0].revents | POLLIN) {
            int ret = read_from(&info);
            if (ret == 1) {
                print_message(&info);
            }
            info.set_read();
        } 
        if (fds[0].revents | POLLOUT) {
            write_to(&info);
        }
    }

    return 0;
}