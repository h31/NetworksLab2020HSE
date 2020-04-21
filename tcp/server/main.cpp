#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <string>
#include <pthread.h>
#include <vector>
#include <ctime>
#include <atomic>

std::vector<pthread_t*> threads_to_join;
pthread_mutex_t clients_mutex;
std::vector<int> clients_sockets;

void send_message_to_all_clients(const char *text_message, const char *username) {
    pthread_mutex_lock(&clients_mutex);

    uint32_t message_length = strlen(text_message);
    uint32_t username_length = strlen(username);
    std::time_t cur_time = std::time(nullptr);

    std::vector<char> buffer;
    buffer.insert(buffer.end(), (char *) &cur_time, ((char *) &cur_time) + sizeof(cur_time));
    buffer.insert(buffer.end(), (char *) &message_length, ((char *) &message_length) + sizeof(message_length));
    buffer.insert(buffer.end(), (char *) &username_length, ((char *) &username_length) + sizeof(username_length));
    buffer.insert(buffer.end(), text_message, text_message + message_length);
    buffer.insert(buffer.end(), username, username + username_length);

    for (int client_socket : clients_sockets) {
        int n = write(client_socket, buffer.data(), buffer.size());
        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }

    }
    pthread_mutex_unlock(&clients_mutex);
}

void *client_process(void* arg) {
    char buffer[256];
    int newsockfd = *(int *)arg;
    delete (int*) arg;
    while (true) {
        bzero(buffer, 256);
        std::vector<char> client_message;
        static const int header_size = 2 * sizeof (uint32_t);
        while (client_message.size() < header_size) {
            int n = read(newsockfd, buffer, 255);
            if (n < 0) {
                perror("ERROR reading from socket");
                exit(1);
            }
            client_message.insert(client_message.end(), buffer, buffer + n);
        }
        uint32_t message_length = *((uint32_t*) client_message.data());
        uint32_t username_length = *((uint32_t*) &client_message.data()[sizeof(uint32_t)]);

        while (client_message.size() < header_size + message_length + username_length) {
            int n = read(newsockfd, buffer, 255);
            if (n < 0) {
                perror("ERROR reading from socket");
                exit(1);
            }
            client_message.insert(client_message.end(), buffer, buffer + n);
        }
        std::vector<char> text_message = std::vector<char>(client_message.begin() + header_size,
                                                           client_message.begin() + header_size + message_length);
        text_message.push_back('\0');
        std::vector<char> username = std::vector<char>(client_message.begin() + header_size + message_length,
                                                       client_message.begin() + header_size + message_length + username_length);
        username.push_back('\0');

        printf("[%s] %s\n", username.data(), text_message.data());

        send_message_to_all_clients(text_message.data(), username.data());
    }
    return nullptr;
}

// accepts new client sockets
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
    pthread_create(thread, nullptr, client_process, (void*) newsockfd);
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
