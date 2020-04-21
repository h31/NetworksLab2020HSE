#include <cstdio>
#include <cstdlib>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <cerrno>

#include <cstring>

#include <iostream>
#include <vector>
#include <thread>
#include <atomic>

#include "../utils.h"

std::atomic_bool stop(false);

void writing_func(int sockfd, const char* name) {
    while (!stop) {
        std::string input;
        getline(std::cin, input);
        if (input.size() > std::numeric_limits<uint16_t>::max()) {
            std::cerr << "input is too long" << std::endl;
        }

        if (input == "exit()") {
            stop = true;
            break;
        }

        int err = write_buffer_to_socket(sockfd, get_text_message(name, input.c_str()));
        if (err < 0) {
            std::cerr << "error during sending message: " << strerror(err) << std::endl;
            stop = true;
            break;
        }
    }
}

void reading_func(int sockfd) {
    while (!stop) {
        std::vector<uint8_t> name, text;
        int err = read_full_message(sockfd, name, text);
        if (err < 0) {
            std::cerr << "error during reading message: " << strerror(err) << std::endl;
            stop = true;
            break;
        }
        std::cout << "[" + std::string(name.begin(), name.end()) + "] "
                  << std::string(text.begin(), text.end()) << std::endl;
    }
}

int main(int argc, char *argv[]) {
    int sockfd, err;

    if (argc < 4) {
        fprintf(stderr, "usage %s hostname port name\n", argv[0]);
        exit(0);
    }

    char* hostname = argv[1];
    char* port = argv[2];
    char* name = argv[3];

    struct addrinfo hints = {}, *addrs;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    err = getaddrinfo(hostname, port, &hints, &addrs); // gethostbyname is deprecated
    if (err != 0)
    {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    for(struct addrinfo *addr = addrs; addr != NULL; addr = addr->ai_next)
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

    std::thread reading = std::thread(reading_func, sockfd);
    std::thread writing = std::thread(writing_func, sockfd, name);

    reading.join();
    writing.join();

    return 0;
}
