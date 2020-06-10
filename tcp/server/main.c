#include <stdio.h>
#include <stdlib.h>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string.h>
#include <time.h>

#include <protocol_utils.h>

#include <sys/poll.h>
#include <fcntl.h>

int pipe_fd[2];

int setup_pipe() {
    int rc = pipe(pipe_fd);
    if (rc < 0) {
        return rc;
    }
    rc = fcntl(pipe_fd[0], F_SETFL, O_NONBLOCK);
    if (rc < 0) {
        return rc;
    }
    rc = fcntl(pipe_fd[1], F_SETFL, O_NONBLOCK);
    return rc;
}

#define SET_SIZE 256

struct ConnectionsSet_t {
    int32_t size;
    int32_t holes;
    struct pollfd fds[SET_SIZE];
    IncompliteBuffer* iBuffers_ptr[SET_SIZE];
};
typedef struct ConnectionsSet_t ConnectionsSet;

void init_set(ConnectionsSet *set) {
    set->size = 0;
    set->holes = 0;
    memset(set->fds, -1, sizeof(set->fds));
}

void add(ConnectionsSet *set, int new_socket, short flags) {
    uint32_t n = set->size;
    set->fds[n].fd = new_socket;
    set->fds[n].events = flags;
    set->iBuffers_ptr[n] = malloc(sizeof(IncompliteBuffer));
    clear_buffer(set->iBuffers_ptr[n]);
    ++set->size;
}

void delete(ConnectionsSet *set, int i) {
    set->fds[i].fd = -1;
    set->fds[i].events = 0;
    set->fds[i].revents = 0;
    free(set->iBuffers_ptr[i]);
    set->holes += 1;
}

void squizze(ConnectionsSet *set) {
    if (set->holes > 0) {
        int empty_i = -1;
        for (int i = 0; i < set->size; i++) {
            if (set->fds[i].fd != -1) {
                if (empty_i != -1) {
                    set->fds[empty_i] = set->fds[i];
                    set->iBuffers_ptr[empty_i] = set->iBuffers_ptr[i];
                    ++empty_i;
                }
            } else {
                if (empty_i == -1) {
                    empty_i = i;
                }
            }
        }
        set->size -= set->holes;
        set->holes = 0;
    }
}

int main(int argc, char *argv[]) {

    if (setup_pipe() < 0) {
        perror("pipe error");
        exit(1);
    }

    int sockfd;
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

    if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &(int){1}, sizeof(int)) < 0) {
        perror("ERROR on setsockopt");
        exit(1);
    }
    
    if (fcntl(sockfd, F_SETFL, O_NONBLOCK) < 0) {
        perror("ERROR on set socket NONBLOCK");
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
    add(&connections, sockfd, POLLIN);

    // 30 sec
    int timeout = (30 * 1000);
    int rc = 0;
    
    int server_end = 0;

    while(!server_end) {
        rc = poll(connections.fds, connections.size, timeout);
        if (rc <= 0) {
            perror("poll() failed");
            break;
        }
        //reading message
        for (int32_t i = 0; i < connections.size; i++) {
            if ((connections.fds[i].revents & POLLIN) == 0) {
                continue;
            }
            if (connections.fds[i].fd == sockfd) {
                // accept
                while (1) {
                    int newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
                    if (newsockfd < 0) {
                        if (errno != EWOULDBLOCK) {
                            perror("ERROR on accept");
                            exit(1);
                        }
                        break;
                    }
                    server_end = (newsockfd == 0);
                    printf("new connection!\n");
                    add(&connections, newsockfd, POLLIN | POLLOUT);
                }
            } else {
                // users
                int close_user_connections = 0;
                IncompliteBuffer *user_iBuffer_ptr = connections.iBuffers_ptr[i];
                struct pollfd *user_fd_ptr = &connections.fds[i];
                while(user_iBuffer_ptr->actual_size != user_iBuffer_ptr->target_size) {
                    rc = read_bytes(user_fd_ptr->fd, user_iBuffer_ptr);
                    if (rc <= 0) {
                        if (errno != EWOULDBLOCK) {
                            if (rc != 0) {
                                perror("read failed");
                            }
                            close_user_connections = 1;
                        }
                        break;
                    }
                    
                }

                if (user_iBuffer_ptr->actual_size == user_iBuffer_ptr->target_size) {
                    message *msg_ptr = (message *) calloc(1, sizeof(message));
                    deserialize_msg_from_iBuffer(msg_ptr, user_iBuffer_ptr);

                    struct tm *timeinfo = localtime(&msg_ptr->tm);
                    printf("<%i:%i> [%s] %s\n", timeinfo->tm_hour, timeinfo->tm_min, msg_ptr->name, msg_ptr->text);

                    write(pipe_fd[1], &msg_ptr, sizeof(message *));
                    clear_buffer(user_iBuffer_ptr);
                }

                if (close_user_connections) {
                    shutdown(user_fd_ptr->fd, SHUT_RDWR);
                    delete(&connections, i);
                }
            }
        }
        
        squizze(&connections);

        // sending all message in queue
        while (1) {
            message *msg_ptr = NULL;
            rc = read(pipe_fd[0], &msg_ptr, sizeof(message*));
            if (rc <= 0) {
                break;
            }
            IncompliteBuffer on_send;
            clear_buffer(&on_send);
            serialize_msg_to_iBuffer(msg_ptr, &on_send);

            for (int32_t i = 0; i < connections.size; i++) {
                if ((connections.fds[i].revents & POLLOUT) == 0) {
                    continue;
                }

                int close_user_connections = 0;

                struct pollfd *user_fd_ptr = &connections.fds[i];
                
                rc = send_bytes(user_fd_ptr->fd, &on_send, 0);
                if (rc <= 0) {
                    close_user_connections = 1;
                }

                if (close_user_connections) {
                    shutdown(user_fd_ptr->fd, SHUT_RDWR);
                    delete(&connections, i);
                }
            
            }
            free(msg_ptr);
            squizze(&connections);
        }
    }

    close(pipe_fd[0]);
    close(pipe_fd[1]);
    return 0;
}
