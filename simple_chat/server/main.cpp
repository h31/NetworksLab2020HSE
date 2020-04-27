#include "../readn.hpp"

#include <vector>

void send_to_all(client_info_t* from, std::vector<client_info_t>& clilents) {
    int len = strlen(from->read_buffer);
    char msg[MSG_LEN_LEN + len];
    bzero(msg, MSG_LEN_LEN + len);
    memcpy(msg, &len, MSG_LEN_LEN);
    memcpy(msg + MSG_LEN_LEN, from->read_buffer, len);

    for (size_t i = 0; i < clilents.size(); i++) {
        clilents[i].set_write(msg, MSG_LEN_LEN + len);
    }
    from->set_read();
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

    int ret = fcntl(sockfd, F_SETFL, O_NONBLOCK);
    if (ret < 0) {
        perror("non blocking set error");
        exit(1);   
    }
    listen(sockfd, 5);

    std::vector<client_info_t> clients_list;

    while (1) {
        pollfd_t fds[clients_list.size() + 1];
        fds[0].fd = sockfd;
        fds[0].events = POLLIN;

        for (size_t i = 0; i < clients_list.size(); i++) {
            fds[i + 1].fd = clients_list[i].socket;
            fds[i + 1].events = POLLIN | POLLOUT;
        }

        int n = poll(fds, clients_list.size() + 1, TIMEOUT);
        if (n < 0) {
            perror("poll ERROR");
            exit(1);
        } else if (n == 0) {
            continue;
        }

        if (fds[0].revents | POLLIN) {
            clilen = sizeof(cli_addr);
            newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &clilen);
            int ret = fcntl(newsockfd, F_SETFL, O_NONBLOCK);
            if (newsockfd < 0 || ret < 0) {
                // perror("non blocking set error");
            } else {
                client_info_t info(newsockfd);
                clients_list.push_back(info);
                printf("Accepted\n");
            }
        }

        for (size_t i = 0; i < clients_list.size(); i++) {
            client_info_t* current = &clients_list[i];
            if (fds[i + 1].revents | POLLIN) {
                int res = read_from(current);
                if (res == 1) {
                    send_to_all(current, clients_list);
                }
            }

            if (fds[i + 1].revents | POLLOUT) {
                write_to(current);
            }
        }
    }

    return 0;
}
