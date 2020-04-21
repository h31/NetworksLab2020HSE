#include <cstdio>
#include <cstdlib>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <cstring>
#include <iostream>
#include <unordered_set>
#include <thread>
#include <vector>

#include "../utils.h"

std::mutex mutex;
std::unordered_set<int> clients;

void handling_client_func(int sockfd) {
    while (true) {
        std::vector<uint8_t> name, text;
        int err = read_full_message(sockfd, name, text);
        if (err < 0) {
            std::cerr << "error during reading message: " << strerror(err) << std::endl;
            continue;
        }

        mutex.lock();

        std::string name_str {name.begin(), name.end()};
        std::string text_str {text.begin(), text.end()};

        for (int client: clients) {
            err = write_buffer_to_socket(client, get_text_message(name_str.c_str(), text_str.c_str()));
            if (err < 0) {
                std::cerr << "error during sending message: " << strerror(err) << std::endl;
            }
        }

        mutex.unlock();
    }
}

int main(int argc, char *argv[]) {
    std::cerr << "server started" << std::endl;

    int sockfd, newsockfd;
    uint16_t portno;
    unsigned int clilen;
    char buffer[256];
    sockaddr_in serv_addr, cli_addr;
    ssize_t n;

    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(0);
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

    std::vector<std::thread> threads;

    while (true) {
        newsockfd = accept(sockfd, (sockaddr *) &cli_addr, &clilen);
        if (newsockfd < 0) {
            std::cerr << "error on accept:" << strerror(errno) << std::endl;
        }

        mutex.lock();
        clients.insert(newsockfd);
        threads.push_back(std::thread(handling_client_func, newsockfd));
        mutex.unlock();
    }

    return 0;
}
