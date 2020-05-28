#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <string.h>

#include <protocol_utils.h>
#include <pthread.h>

void end_of_connection(int socket, int verbose) {
    shutdown(socket, 2);
    if (verbose) {
        printf("Close connection\n");
    }
}

int fix_name(char *name) {
    uint32_t len = strlen(name);
    if (len > 0 && name[len - 1] == '\n') {
        name[len - 1] = '\0';
        return 1;
    }
    return 0;
}

void *reader(void *sct) {
    int socket = *(int *) sct;

    message msg;
    char buffer[BUFFER_SIZE];
    while (1) {
        int n = read_bytes(socket, buffer);

        if (n < 0) {
            perror("ERROR reading from socket");
            return (void *)1;
        }

        if (n == 0) {
            end_of_connection(socket, 0);
            return (void *)0;
        }
        deserialize_msg(&msg, buffer, n);
        struct tm *timeinfo = localtime(&msg.tm);
        printf("<%i:%i> [%s] %s\n", timeinfo->tm_hour, timeinfo->tm_min, msg.name, msg.text);
    }
    return (void *)0;
}

int main(int argc, char *argv[]) {
    int sockfd, err;
    //uint16_t portno;
    //struct sockaddr_in serv_addr;
    //struct hostent *server;

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

    err = getaddrinfo(hostname, port, &hints, &addrs); // gethostbyname is deprecated
    if (err != 0)
    {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    for (struct addrinfo *addr = addrs; addr != NULL; addr = addr->ai_next) {
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

    pthread_t tid;
    pthread_attr_t attr;

    pthread_attr_init(&attr);

    pthread_create(&tid, &attr,reader, &sockfd);

    int code_of_connection = 1;

    message msg;
    printf("Please enter the nickname: ");
    bzero(msg.name, NAME_SIZE);
    char* pname = fgets(msg.name, NAME_SIZE, stdin);
    if (pname == NULL) {
        code_of_connection = 0;
    } else { 
        fix_name(msg.name);
    }

    while(code_of_connection > 0) {
        bzero(msg.text, TEXT_SIZE);
        char* pnt = fgets(msg.text, TEXT_SIZE, stdin);

        if (pnt == NULL) {
            code_of_connection = 0;
            break;
        }

        time ( &msg.tm );
        
        char buffer[BUFFER_SIZE];
        bzero(buffer, BUFFER_SIZE);
        uint32_t len = serialize_msg(&msg, buffer);

        code_of_connection = send_bytes(sockfd, buffer, len);

        if (code_of_connection < 0) {
            perror("ERROR writing to socket");
            break;
        }
    }
    end_of_connection(sockfd, 1);
    pthread_join(tid, NULL);
    return 0;
}
