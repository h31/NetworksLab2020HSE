#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <string.h>

#include <protocol_utils.h>

void end_of_connection(int socket, int verbose) {
    shutdown(socket, 2);
    if (verbose) {
        printf("Close connection\n");
    }
}

int fix_name(char *name) {
    size_t len = strlen(name);
    if (len > 0 && name[len - 1] == '\n') {
        name[len - 1] = '\0';
        return 1;
    }
    return 0;
}

void *reader(void *sct) {
    int socket = *(int *) sct;

    char buffer[BUFFER_SIZE];
    while (1) {
        /* Now read server response */
        bzero(buffer, BUFFER_SIZE);
        int n = read_msg(socket, buffer);

        if (n < 0) {
            perror("ERROR reading from socket");
            return (void *)1;
        }

        if (n == 0) {
            end_of_connection(socket, 0);
            return (void *)0;
        }

        printf("\n%s\n", buffer);
    }
    return (void *)0;
}

int main(int argc, char *argv[]) {
    int sockfd, err;
    //uint16_t portno;
    //struct sockaddr_in serv_addr;
    //struct hostent *server;

    char buffer[BUFFER_SIZE];

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

    pthread_t tid;
    pthread_attr_t attr;

    pthread_attr_init(&attr);

    pthread_create(&tid, &attr,reader, &sockfd);

    /* Now ask for a message from the user, this message
       * will be read by server
    */
    int code_of_connection = 1;

    char name[BUFFER_SIZE];
    printf("Please enter the nickname: ");
    bzero(name, BUFFER_SIZE);
    char* pname = fgets(name, BUFFER_SIZE - 1, stdin);
    if (pname == NULL) {
        code_of_connection = 0;
    } else { 
        fix_name(name);
        code_of_connection = send_msg(sockfd, name);
    }

    while(code_of_connection > 0) {
        printf("Please enter the message: ");
        bzero(buffer, BUFFER_SIZE);
        char* pnt = fgets(buffer, BUFFER_SIZE - 1, stdin);

        if (pnt == NULL) {
            code_of_connection = 0;
            break;
        }
        /* Send message to the server */
        code_of_connection = send_msg(sockfd, buffer);

        if (code_of_connection < 0) {
            perror("ERROR writing to socket");
            break;
        }
    }
    end_of_connection(sockfd, 1);
    pthread_join(tid);
    return 0;
}
