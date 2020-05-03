#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <time.h>

#include <protocol_utils.h>
#include <pthread.h>
int main(int argc, char *argv[]) {
    int sockfd, newsockfd;
    uint16_t portno;
    unsigned int clilen;
    char buffer[256];
    struct sockaddr_in serv_addr, cli_addr;
    ssize_t n;

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
    
    clilen = sizeof(cli_addr);

    newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
    if (newsockfd < 0) {
        perror("ERROR on accept");
        exit(1);
    }

    char name[256];
    bzero(name, 256);
    n = read_msg(newsockfd, name); // recv on Windows

    if (n == 0) {
        printf("End of connection\n");
        return 0;
    }
    
    printf("%s connected!\n", name);
    
    while(1) {


        bzero(buffer, 256);
        n = read_msg(newsockfd, buffer); // recv on Windows

        if (n == 0) {
            printf("End of connection\n");
            break;
        }

        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }

        time_t rawtime;
        struct tm * timeinfo;

        time ( &rawtime );
        timeinfo = localtime ( &rawtime );
        time ( &rawtime );
        timeinfo = localtime ( &rawtime );

        printf("<%i:%i> [%s]: %s\n", timeinfo->tm_hour, timeinfo->tm_min, name, buffer);

        n = send_msg(newsockfd, "I got your message"); // send on Windows

        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
    }
    return 0;
}
