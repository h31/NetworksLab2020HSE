//
// Created by ralsei on 20.04.2020.
//

#include "Client.h"

Client::Client(int socketDescriptor, Server *server) : socketDescriptor(socketDescriptor), server(server) {
}

void Client::readingMessages() {
    if (currentMessage == nullptr) {
        currentMessage = new Message();
    }

    auto failed = currentMessage->readFromClientSocket(socketDescriptor);

    if (failed) {
        server->removeClient(this);
        return;
    }

    if (currentMessage->fullyRead()) {
        server->broadcastToAllClients(currentMessage);

        delete currentMessage;
        currentMessage = new Message();
    }
}

void Client::writingMessages() {
    Message *messageToSend = messagesToSend.front();
    auto result = messageToSend->writeToClientSocket(socketDescriptor);
    if (result < 0) {
        server->removeClient(this);
        return;
    }

    if (messageToSend->fullyRead()) {
        messagesToSend.pop();

        if (messagesToSend.empty()) {
            server->removeWriteRegistration(this);
        }
    }

    delete messageToSend;
}

void Client::submitNewMessageToQueue(Message *message) {
    messagesToSend.push(message);
}

Client::~Client() {
    shutdown(socketDescriptor, SHUT_RDWR);
    close(socketDescriptor);
    delete currentMessage;
}


