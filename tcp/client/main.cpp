#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <string.h>
#include <thread>

void read_loop(int sockfd) {
    char buffer[256];

    while(true) {
        /* Now read server response */
        bzero(buffer, 256);
        int n = read(sockfd, buffer, 255);

        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }

        printf("%s", buffer);
    }
}

int writen(int fd, const char *buf, int len) {
    int count = 0;
    while(len != 0){
        int n = write(fd, buf, len);
        if(n < 0) {
            perror("CANNOT WRITE");
            exit(-1);
        } else if (n == 0) {
            perror("CONNECTION CLOSED");
            exit(-1);
        }
        len -= n;
        buf += n;
        count += n;
    }
    return count;
}

int main(int argc, char *argv[]) {
    int sockfd, n, err;
    //uint16_t portno;
    //struct sockaddr_in serv_addr;
    //struct hostent *server;

    char buffer[256];

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
        fprintf(stderr, "A %s: %s\n", hostname, gai_strerror(err));
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
        fprintf(stderr, "B %s: %s\n", hostname, strerror(err));
        exit(1);
    }

    //std::thread messages(read_loop, sockfd);

    while(true) {
        /* Now ask for a message from the user, this message
           * will be read by server
        */

        bzero(buffer, 256);
        fgets(buffer, 255, stdin);

        /* Send message to the server */

        char buf[4];
        *(uint32_t *)buf = htonl((uint32_t)strlen(buffer));

        n = writen(sockfd, buf, 4);

        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }

        n = writen(sockfd, buffer, strlen(buffer));

        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
    }

    return 0;
}
