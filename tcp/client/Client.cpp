#include "Client.h"

#include "../serialization.h"
#include "../ioutils.h"

#include <cstdio>
#include <cstdlib>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <cerrno>

#include <cstring>
#include <iostream>
#include <atomic>

namespace {
    void printMessage(std::time_t time, const std::string& name, const std::string& message) {
        char buffer[10];
        std::tm* ptm = std::localtime(&time);
        std::strftime(buffer, 10, "%H:%M", ptm);
        printf("<%s> [%s] %s\n", buffer, name.c_str(), message.c_str());
    }
}

Client::Client(const char* hostname, const char* port, const char* name) : m_name(name) {
    addrinfo hints = {};
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    addrinfo* addrs;
    int err = getaddrinfo(hostname, port, &hints, &addrs);
    if (err != 0) {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    int sockfd = -1;
    for (addrinfo* addr = addrs; addr != nullptr; addr = addr->ai_next) {
        sockfd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (sockfd == -1) {
            err = errno;
            break;
        }

        if (connect(sockfd, addr->ai_addr, addr->ai_addrlen) == 0) {
            break;
        }

        err = errno;

        close(sockfd);
        sockfd = -1;
    }

    freeaddrinfo(addrs);

    if (sockfd == -1) {
        fprintf(stderr, "%s: %s\n", hostname, strerror(err));
        exit(1);
    }

    m_socketFD = sockfd;

    m_reader = std::thread(&Client::read_routine, this);
    m_writer = std::thread(&Client::write_routine, this);
    m_reader.join();
    m_writer.join();
}

Client::~Client() {
    stop();
}

std::optional<std::string> Client::readString(char (& lengthBuffer)[4]) const {
    if (readFullBuffer(m_socketFD, lengthBuffer, 4) == 0) {
        const auto nameLength = intFromArray<uint32_t>(lengthBuffer);
        std::vector<char> str(nameLength + 1, 0);
        if (readFullBuffer(m_socketFD, str.data(), nameLength) == 0) {
            return std::string(str.data());
        }
    }
    return {};
}

std::optional<std::time_t> Client::readTime(char (& timeBuffer)[8]) const {
    if (readFullBuffer(m_socketFD, timeBuffer, 8) == 0) {
        return static_cast<std::time_t>(intFromArray<uint64_t>(timeBuffer));
    }
    return {};
}

void Client::read_routine() {
    char timeBuffer[8];
    char lengthBuffer[4];
    while (true) {
        const auto time = readTime(timeBuffer);
        const auto name = readString(lengthBuffer);
        const auto message = readString(lengthBuffer);
        if (time && name && message) {
            printMessage(time.value(), name.value(), message.value());
            continue;
        }

        std::cout << "<Exit. Please press enter.>" << std::endl;
        stop();
        return;
    }
}

void Client::write_routine() {
    while (true) {
        std::string input;
        std::getline(std::cin, input);
        printf("\033[A");
        printf("\33[2K");

        uint32_t nameLength = m_name.length();
        uint32_t messageLength = input.length();
        uint32_t totalLength = 8 + nameLength + messageLength;

        if (writeFullBuffer(m_socketFD, intToArray(totalLength).data(), 4) == 0
            && writeFullBuffer(m_socketFD, intToArray(nameLength).data(), 4) == 0
            && writeFullBuffer(m_socketFD, m_name.data(), nameLength) == 0
            && writeFullBuffer(m_socketFD, intToArray(messageLength).data(), 4) == 0
            && writeFullBuffer(m_socketFD, input.data(), messageLength) == 0) {
            continue;
        }
        stop();
        return;
    }
}

void Client::stop() {
    static std::atomic_bool stopped(false);
    if (stopped) {
        return;
    }
    stopped.store(true);
    shutdown(m_socketFD, SHUT_RDWR);
    close(m_socketFD);
}
