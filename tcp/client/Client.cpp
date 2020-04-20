//
// Created by ralsei on 20.04.2020.
//

#include "Client.h"

#include <utility>
#include <netdb.h>

Client::Client(char* hostname, char* port, std::string name): name(std::move(name)) {
    addrinfo hints = {}, *addrs;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    int err = getaddrinfo(hostname, port, &hints, &addrs);
    if (err != 0) {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    int sockfd = -1;
    for (struct addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next) {
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

    serverSocketDescriptor = sockfd;

    pthread_mutex_init(messagesQueueMutex, nullptr);
    pthread_cond_init(queueNotEmpty, nullptr);

    readFromConsoleThread = std::thread(&Client::readingMessagesFromConsole, this);
    writeToServerThread = std::thread(&Client::writingMessages, this);
    readFromServerThread = std::thread(&Client::readingMessagesFromServer, this);

    readFromConsoleThread.join();
}

void Client::readingMessagesFromServer() {
    while (isWorking()) {
        auto message = Message(&interrupted);

        bool failed = message.readFromServer(serverSocketDescriptor);

        if (failed) {
            perror("ERROR connecting to server.");
            leave();
        }

        message.printMessage();
    }
}

void Client::readingMessagesFromConsole() {
    while (isWorking()) {
        std::string messageLine;
        std::getline(std::cin, messageLine);

        auto messageToSend = new Message(name.length(), (uint8_t *) name.c_str(), messageLine.length(),
                                         (uint8_t *) messageLine.c_str(), &interrupted);
        submitNewMessage(messageToSend);
    }
}

void Client::writingMessages() {
    while (isWorking()) {
        pthread_mutex_lock(messagesQueueMutex);
        while (isWorking() && messagesToSend.empty()) {
            pthread_cond_wait(queueNotEmpty, messagesQueueMutex);
        }

        Message* messageToSend = messagesToSend.front();
        messagesToSend.pop();
        pthread_mutex_unlock(messagesQueueMutex);

        bool failed = messageToSend->writeToServerSocket(serverSocketDescriptor);
        //delete messageToSend;

        if (failed) {
            perror("ERROR connecting to server.");
            leave();
        }
    }
}

void Client::leave() {
    exit(1);
}

void Client::submitNewMessage(Message* message) {
    pthread_mutex_lock(messagesQueueMutex);
    messagesToSend.push(message);

    if (messagesToSend.size() == 1) {
        pthread_cond_broadcast(queueNotEmpty);
    }

    pthread_mutex_unlock(messagesQueueMutex);
}

Client::~Client() {
    pthread_mutex_destroy(messagesQueueMutex);
    pthread_cond_destroy(queueNotEmpty);

    shutdown(serverSocketDescriptor, SHUT_RDWR);
    close(serverSocketDescriptor);
    interrupted = true;

    readFromConsoleThread.join();
    writeToServerThread.join();
    readFromServerThread.join();

    delete messagesQueueMutex;
    delete queueNotEmpty;
}

bool Client::isWorking() const {
    return !interrupted;
}
