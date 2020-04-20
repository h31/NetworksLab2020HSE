#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <pthread.h>

void *client_pocess(void* arg) {
    char buffer[256];
    int newsockfd = *(int *)arg;
    while (true) {
        bzero(buffer, 256);
        ssize_t n = read(newsockfd, buffer, 255);

        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }

        printf("Here is the message: %s\n", buffer);

        n = write(newsockfd, "I got your message", 18);

        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
    }
}

void server_loop(int sockfd) {
    sockaddr_in cli_addr;

    unsigned int clilen = sizeof(cli_addr);
    // TODO free it
    int *newsockfd = new int(0);
    *newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);

    if (*newsockfd < 0) {
        perror("ERROR on accept");
        exit(1);
    }
    // TODO join it somewhere
    pthread_t thread;
    // TODO process res
    int res = pthread_create( &thread, NULL, client_pocess, (void*) newsockfd);

}



int main(int argc, char *argv[]) {
    int sockfd;
    uint16_t portno;
    struct sockaddr_in serv_addr;

    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    if (sockfd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    portno = (uint16_t) atoi(argv[1]);

    /* Initialize socket structure */
    bzero((char *) &serv_addr, sizeof(serv_addr));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);

    if (bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR on binding");
        exit(1);
    }

    listen(sockfd, 5);
    while (true) {
        server_loop(sockfd);
    }
    close(sockfd);
    return 0;
}
