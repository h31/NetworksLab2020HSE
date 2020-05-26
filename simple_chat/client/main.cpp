#include "../readn.hpp"

int8_t utc;

void* sender(void* arg) {
    int socket = *((int*) arg); 

    char buffer[MAX_MSG_LEN];
    char msg[MSG_LEN];
    char name[NAME_LEN];
    utc = 1000;


    printf("Enter your name: ");
    bzero(name, NAME_LEN);
    fgets(name, NAME_LEN - 1, stdin);
    
    int utc_buff;
    printf("Enter your time zone (UTC<N>): ");
    scanf("%i", &utc_buff);
    utc = utc_buff;

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

        int message_len = strlen(msg);
        int len = strlen(buffer) + 1;
        time_t rawtime;
        time(&rawtime);
        struct tm* timeinfo = localtime(&rawtime);
        uint32_t time = timeinfo->tm_hour * SIH  + timeinfo->tm_min * SIM + timeinfo->tm_sec;
        message_t message(len, utc, time, buffer);
        
        if (message_len <= 1) {
            continue;
        }
        
        int n = message.send_to_socket(socket);

        if (n < 0) {
            perror("ERROR writing to socket");
            close(socket);
            exit(1);
        }
    }
}

void* getter(void* arg) {
    int socket = *((int*) arg);


    while (1) {
        message_t message;
        int n = message.read_from_socket(socket);

        if (n < 0) {
            perror("ERROR reading from socket");
            close(socket);
            exit(1);
        }

        if (message.size == 0) {
            continue;
        }

        uint32_t len = message.len_int_string_format();
        char buffer[len];
        message.to_string(utc, buffer);
        printf("%s", buffer);
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