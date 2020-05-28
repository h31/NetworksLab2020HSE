#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <time.h>

#include <protocol_utils.h>
#include <pthread.h>

int pipe_fd[2];
// pthread_mutex_t pipe_lock;

#define SET_SIZE 256

struct ConnectionsSet_t {
    pthread_mutex_t lock;
    uint32_t size;
    int sockets[SET_SIZE];
};
typedef struct ConnectionsSet_t ConnectionsSet;

void init_set(ConnectionsSet *set) {
    pthread_mutex_init(&set->lock, NULL);
    set->size = 0;
    memset(set->sockets, -1, SET_SIZE * sizeof(int));
}

void add(ConnectionsSet *set, int new_socket) {
    pthread_mutex_lock(&set->lock);
    set->sockets[set->size++] = new_socket;
    pthread_mutex_unlock(&set->lock);
}


void *sending(void* conn_set) {
    ConnectionsSet *connections = (ConnectionsSet *) conn_set;
    while (1) {
        message *msg = NULL;
        int nread = 0;
        nread = read(pipe_fd[0], &msg, sizeof(message*));
        if (nread == -1) {
            continue;
        }
        if (msg == NULL) {
            break;
        }
        char buffer[BUFFER_SIZE];
        uint32_t msg_len = serialize_msg(msg, buffer);

        pthread_mutex_lock(&connections->lock);
        int empty_i = -1;

        struct tm *timeinfo = localtime(&msg->tm);
        printf("<%i:%i> [%s]: %s\n", timeinfo->tm_hour, timeinfo->tm_min, msg->name, msg->text);
        fflush(stdout);
        
        uint32_t len = connections->size;
        for (uint32_t i = 0; i < len; i++) {
            if (send_bytes(connections->sockets[i], buffer, msg_len) > 0) {
                if (empty_i != -1) {
                    connections->sockets[empty_i++] = connections->sockets[i];
                } 
            } else {
                if (empty_i == -1) {
                    empty_i = i;
                }
                --connections->size;
            }
        }
        pthread_mutex_unlock(&connections->lock);
        free(msg);
    }
    return NULL;
}

void *connection_handler(void *sct) {
    int socket = (int) sct;

    int n = 1;

    printf("new connection!\n");
    fflush(stdout);
    char buffer[BUFFER_SIZE];

    while(1) {

        message *msg = (message *) calloc(1, sizeof(message));
        n = read_bytes(socket, buffer);

        if (n == 0) {
            printf("End of connection\n");
            write(socket, "lol", 3);
            break;
        }

        if (n < 0) {
            perror("ERROR reading from socket");
            exit(1);
        }

        deserialize_msg(msg, buffer, n);
        write(pipe_fd[1], &msg, sizeof(message *));
        
        if (n < 0) {
            perror("ERROR writing to socket");
            exit(1);
        }
    }
    shutdown(socket, 2);
    return (void *)0;
}

int main(int argc, char *argv[]) {

    if (pipe(pipe_fd) < 0) {
        perror("pipe error");
        exit(1);
    }

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
    
    
    ConnectionsSet connections;
    init_set(&connections);

    pthread_t sending_tid;
    pthread_attr_t sending_attr;

    pthread_attr_init(&sending_attr);

    pthread_create(&sending_tid, &sending_attr, sending, &connections);

    while(1) {
        newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
        if (newsockfd < 0) {
            perror("ERROR on accept");
            exit(1);
        }
        
        add(&connections, newsockfd);
        pthread_t reader_tid;
        pthread_attr_t reader_attr;

        pthread_attr_init(&reader_attr);

        pthread_create(&reader_tid, &reader_attr, connection_handler, (void *)newsockfd);
    }

    close(pipe_fd[0]);
    close(pipe_fd[1]);
    return 0;
}
