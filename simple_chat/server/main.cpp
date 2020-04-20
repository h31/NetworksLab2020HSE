#include "../readn.hpp"

typedef struct clients
{
    pthread_mutex_t mutex;
    int size;
    int* sockets;

    clients() {
        size = 100;
        pthread_mutex_init(&mutex, NULL);
        sockets = (int*) calloc(size, sizeof(int));
    }

    ~clients() {
        free(sockets);
        pthread_mutex_destroy(&mutex);
    }

    int addClient(int socket) {
        for (int i = 0; i < size; i++) {
            if (sockets[i] == 0) {
                pthread_mutex_lock(&mutex);
                sockets[i] = socket;
                pthread_mutex_unlock(&mutex);
                return i;
            }
        }
        resize();
        return addClient(socket);
    }

    void resize() {
        pthread_mutex_lock(&mutex);
        size *= 2;
        sockets = (int*) realloc(sockets, sizeof(int) * size);
        memset(sockets + size/2, 0, size/2);
        pthread_mutex_unlock(&mutex);
    }

    void remove(int index) {
        pthread_mutex_lock(&mutex);
        close(sockets[index]);
        sockets[index] = 0;
        pthread_mutex_unlock(&mutex);
    }
} clients_t ;

typedef struct client_info
{
    clients_t* clients_list;
    int sockfd;
} client_info_t;


void* client(void* arg) {
    client_info_t* info = (client_info_t*) arg;
    clients_t* clients_list = info->clients_list;
    int num = clients_list->addClient(info->sockfd);

    char buffer[MAX_MSG_LEN];

    while(1) {
        bzero(buffer, MAX_MSG_LEN);
        int n = getmessage(info->sockfd, buffer);
        if (n < 0) {
            clients_list->remove(num);
            return 0;
        }

        // printf("got message: %s\n", buffer);

        pthread_mutex_lock(&(clients_list->mutex));

        for (int i = 0; i < clients_list->size; i++) {
            if (clients_list->sockets[i] != 0) {
                sendmessage(clients_list->sockets[i], strlen(buffer) + 1, buffer);
            }
        }

        pthread_mutex_unlock(&(clients_list->mutex));
    }
}

int main(int argc, char *argv[]) {
    int sockfd, newsockfd;
    uint16_t portno;
    unsigned int clilen;
    struct sockaddr_in serv_addr, cli_addr;

    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    if (sockfd < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    int tmp = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &tmp, sizeof(int));

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

    clients_t clients_list;

    while (1) {
        clilen = sizeof(cli_addr);
        newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);

        if (newsockfd < 0) {
            perror("ERROR on accept");
            continue;
        }

        client_info_t info;
        info.clients_list = &clients_list;
        info.sockfd = newsockfd;

        pthread_t thread;
        printf("connected %i\n", newsockfd);
        pthread_create(&thread, 0, client, (void*) &info);
    }

    return 0;
}
