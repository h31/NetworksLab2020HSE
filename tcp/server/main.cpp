#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <string>
#include <pthread.h>
#include <vector>
#include <sstream>
#include <ctime>

std::vector<pthread_t*> threads_to_join;
pthread_mutex_t clients_mutex;
std::vector<int> clients_sockets;

void send_message_to_all_clients(const char *text_message, const char *username) {
    pthread_mutex_lock(&clients_mutex);

    uint32_t message_length = strlen(text_message);
    uint32_t username_length = strlen(username);
    std::stringbuf buf;
    std::time_t cur_time = std::time(nullptr);

    buf.sputn((char *) &cur_time, sizeof cur_time);
    buf.sputn((char *) &message_length, sizeof message_length);
    buf.sputn((char *) &username_length, sizeof username_length);
    buf.sputn(text_message, message_length);
    buf.sputn(username, username_length);
    for (char c : buf.str()) {
        for (int client_socket : clients_sockets) {
            int n = write(client_socket, &c, 1);
            if (n < 0) {
                perror("ERROR writing to socket");
                exit(1);
            }
        }
        printf("%d\n", (int)c);
    }
    pthread_mutex_unlock(&clients_mutex);
}

void *client_pocess(void* arg) {
    char buffer[256];
    int newsockfd = *(int *)arg;
    delete (int*) arg;
    while (true) {
        bzero(buffer, 256);
        size_t n = 0;

        std::vector<char> v;
        while (n < sizeof(uint32_t)) {
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
        uint32_t message_length = *((uint32_t*) v.data());
        printf("Here is the message: %d\n", message_length);

        n -= sizeof(uint32_t);
        while (n < sizeof(uint32_t)) {
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
        uint32_t username_length = *((uint32_t*) &v.data()[sizeof(uint32_t)]);
        printf("Here is the message: %d\n", username_length);

        n -= sizeof(uint32_t);

        std::string message = "";
        for (int i = 2 * sizeof(uint32_t); i < v.size(); i++) {
            message += v[i];
            perror("here");

        }
        while (n < message_length + username_length) {
            int q = read(newsockfd, buffer, 255);
            if (q < 0) {
                perror("ERROR reading from socket");
                exit(1);
            }
            for (int i = 0; i < q; i++) {
                message += buffer[i];
            }
            n += q;
        }
         std::string text_message = message.substr(0, message_length);
         std::string username = message.substr(message_length, username_length);
         printf("Here is the message: %s\n", text_message.c_str());

         printf("Here is the message: %s\n", username.c_str());


         send_message_to_all_clients(text_message.c_str(), username.c_str());
    }
}

void server_loop(int sockfd) {
    sockaddr_in cli_addr;

    unsigned int clilen = sizeof(cli_addr);
    int *newsockfd = new int(0);
    *newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);

    if (*newsockfd < 0) {
        perror("ERROR on accept");
        exit(1);
    }
    pthread_mutex_lock(&clients_mutex);
    clients_sockets.push_back(*newsockfd);
    pthread_mutex_unlock(&clients_mutex);
    pthread_t *thread = new pthread_t();
    threads_to_join.push_back(thread);
    // TODO process res
    int res = pthread_create(thread, nullptr, client_pocess, (void*) newsockfd);

}


int main(int argc, char *argv[]) {
    int sockfd;
    uint16_t portno;
    struct sockaddr_in serv_addr;

    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

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
    for (pthread_t *thread : threads_to_join) {
        pthread_join(*thread, nullptr);
        delete thread;
    }
    close(sockfd);
    return 0;
}
