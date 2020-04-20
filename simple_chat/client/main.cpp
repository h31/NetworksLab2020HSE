#include "../readn.hpp"

void* sender(void* arg) {
    int socket = *((int*) arg); 

    char buffer[MAX_MSG_LEN];
    char msg[MSG_LEN];
    char name[NAME_LEN];


    printf("Enter your name: ");
    bzero(name, NAME_LEN);
    fgets(name, NAME_LEN - 1, stdin);
    
    for (int i = 0; name[i]; i++) {
        if (name[i] == '\n') {
            name[i] = 0;
            break;
        }
    }

    while (1) {
        bzero(msg, MSG_LEN);
        fgets(msg, MSG_LEN - 1, stdin);

        bzero(buffer, MAX_MSG_LEN);

        strcpy(buffer, name);
        strcat(buffer, " : ");
        strcat(buffer, msg);

        int n = sendmessage(socket, strlen(buffer) + 1, buffer);

        if (n < 0) {
            perror("ERROR writing to socket");
            close(socket);
            exit(1);
        }
    }
}

void* getter(void* arg) {
    int socket = *((int*) arg);

    char buffer[MAX_MSG_LEN];

    while (1) {
        bzero(buffer, MAX_MSG_LEN);
        int n = getmessage(socket, buffer);

        if (n < 0) {
            perror("ERROR reading from socket");
            close(socket);
            exit(1);
        }

        time_t rawtime;
        time(&rawtime);
        struct tm* timeinfo = localtime(&rawtime);

        printf("<%i:%i:%i> %s", timeinfo->tm_hour, timeinfo->tm_min, timeinfo->tm_sec, buffer);
    }
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
    if (err != 0)
    {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    for(struct addrinfo *addr = addrs; addr != NULL; addr = addr->ai_next)
    {
        sockfd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (sockfd == -1)
        {
            err = errno;
            break;
        }

        if (connect(sockfd, addr->ai_addr, addr->ai_addrlen) == 0)
        {
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

    printf("Connected to chat\n");

    pthread_t senderThread, getterThread;
    pthread_create(&senderThread, 0, sender, (void*) &sockfd);
    pthread_create(&getterThread, 0, getter, (void*) &sockfd);

    pthread_join(getterThread, 0);
    pthread_join(senderThread, 0);

    return 0;
}