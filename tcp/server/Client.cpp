//
// Created by ralsei on 20.04.2020.
//

#include "Client.h"

Client::Client(int socketDescriptor, Server *server) : socketDescriptor(socketDescriptor), server(server) {
    pthread_mutex_init(messagesQueueMutex, nullptr);
    pthread_cond_init(queueNotEmpty, nullptr);

    readFromServerThread = std::thread(&Client::readingMessages, this);
    broadcastToAllClientsThread = std::thread(&Client::writingMessages, this);
}

void Client::readingMessages() {
    while (isWorking()) {
        auto message = new Message(&interrupted);
        bool failed = message->readFromClientSocket(socketDescriptor);

        if (failed) {
            server->removeClient(this);
            return;
        }

        submitNewMessageToQueue(message);
    }
}

void Client::writingMessages() {
    while (isWorking()) {
        pthread_mutex_lock(messagesQueueMutex);
        while (isWorking() && messagesToSend.empty()) {
            pthread_cond_wait(queueNotEmpty, messagesQueueMutex);
        }

        Message *messageToSend = messagesToSend.front();
        messagesToSend.pop();
        pthread_mutex_unlock(messagesQueueMutex);

        server->broadcastToAllClients(messageToSend);
        delete messageToSend;
    }
}

void Client::submitNewMessageToQueue(Message *message) {
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

    shutdown(socketDescriptor, SHUT_RDWR);
    close(socketDescriptor);
    interrupted = true;

    readFromServerThread.join();
    broadcastToAllClientsThread.join();
}

bool Client::isWorking() const {
    return !interrupted;
}


