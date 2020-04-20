#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <errno.h>

#include <string.h>
#include <pthread.h>
#include <string>
#include <sstream>
#include <vector>
#include <ctime>

void *read_server_messages(void *arg) {
    char buffer[256];
    int newsockfd = *(int *) arg;
    while (true) {

    bzero(buffer, 256);
    size_t n = 0;

    std::vector<char> v;
    while (n < 2 * sizeof(uint32_t) + sizeof(time_t)) {
        int q = read(newsockfd, buffer, 255);
        if (q < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        for (int i = 0; i < q; i++) {
            v.push_back(buffer[i]);
        }
        n += q;
    }

    uint32_t message_length = *((uint32_t*) &v.data()[sizeof(time_t)]);

    uint32_t username_length = *((uint32_t*) &v.data()[sizeof(uint32_t) + sizeof (uint32_t)]);

    n -= 2 * sizeof(uint32_t) + sizeof(time_t);

    printf("Here is the message: %d\n", message_length + username_length);
    std::string message = "";
    for (int i = 2 * sizeof(uint32_t) + sizeof(time_t); i < v.size(); i++) {
        message += v[i];

    }
    while (n < message_length + username_length) {
        int q = read(newsockfd, buffer, 255);
        if (q < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }
        for (int i = 0; i < q; i++) {
            message += buffer[i];
            printf("%c-", buffer[i]);
        }
        n += q;
    }
    printf("Here is the message: %d\n", message.length());

     std::string text_message = message.substr(0, message_length);
     std::string username = message.substr(message_length, username_length);
     printf("Here is the message: %s\n", text_message.c_str());

     printf("Here is the message: %s\n", username.c_str());

    }
}

void send_message_to_server(int sockfd, char *message, char* username) {
    /* Send message to the server */
    uint32_t message_length = strlen(message);
    uint32_t username_length = strlen(username);
    std::stringbuf buf;
    buf.sputn((char *) &message_length, sizeof message_length);
    buf.sputn((char *) &username_length, sizeof username_length);
    buf.sputn(message, message_length);
    buf.sputn(username, username_length);
    char buffer[256];
    //buf.
    for (char c : buf.str()) {
        // TODO check
        int n = write(sockfd, &c, 1);
        printf("%d\n", (int)c);
    }

}

void client_loop(int sockfd, char *username) {
    char buffer[256];

    /* Now ask for a message from the user, this message
       * will be read by server
    */
    while (true) {
        bzero(buffer, 256);
        fgets(buffer, 255, stdin);
        if (buffer[0] == '\0') {
            return;
        }
        send_message_to_server(sockfd, buffer, username);
    }
}

int main(int argc, char *argv[]) {
//    uint32_t message_length = strlen("1");
//    uint32_t username_length = strlen("username");
//    std::stringbuf buf;
//    buf.sputn((char *) &message_length, sizeof message_length);
////    buf.sputn((char *) &username_length, sizeof username_length);
//    buf.sputn("1", strlen("1"));
//    buf.sputn("username", strlen("username"));
//    for (char c : buf.str()) {
//        printf("%d\n", int (c));
//    }
//    printf("%s\n", buf.str().c_str());
//    uint32_t message = *((uint32_t*) buf.str().c_str());
//    printf("Here is the message: %d\n", message);


    int sockfd, err;

    if (argc < 4) {
        fprintf(stderr, "usage %s hostname port username\n", argv[0]);
        exit(0);
    }

    char* hostname = argv[1];
    char* port = argv[2];
    char* username = argv[3];

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

    for(struct addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next)
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

    pthread_t thread;
    // TODO process res
    int res = pthread_create(&thread, nullptr, read_server_messages, (void*) &sockfd);
    client_loop(sockfd, username);
    pthread_join(thread, nullptr);
     return 0;
}
