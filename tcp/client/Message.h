//
// Created by ralsei on 20.04.2020.
//

#ifndef CLIENT_MESSAGE_H
#define CLIENT_MESSAGE_H

#include <memory>
#include <unistd.h>
#include <cstring>
#include <ctime>
#include <netinet/in.h>
#include <iomanip>
#include <iostream>
#include <cstring>

class Message {
public:
    Message(volatile bool* volatile interrupted) : interrupted(interrupted) {} ;

    Message(uint32_t userNicknameLength, uint8_t *userNickname, uint32_t messageLength, uint8_t *message, volatile bool* volatile interrupted) {
        this->messageLength = messageLength;
        this->interrupted = interrupted;
        this->userNicknameLength = userNicknameLength;
        this->userNickname = new uint8_t[userNicknameLength];
        for (uint32_t i = 0; i < userNicknameLength; i++) {
            this->userNickname[i] = userNickname[i];
        }

        this->message = new uint8_t[messageLength];
        for (uint32_t i = 0; i < messageLength; i++) {
            this->message[i] = message[i];
        }
    };

    ~Message();

    bool readFromClientSocket(int socketDescriptor);

    bool readFromServer(int socketDescriptor);

    bool writeToClientSocket(int socketDescriptor);

    bool writeToServerSocket(int socketDescriptor);

    void printMessage();

private:
    uint32_t userNicknameLength = 0;
    uint8_t *userNickname = nullptr;
    uint32_t messageLength = 0;
    uint8_t *message = nullptr;
    uint32_t time = -1;
    volatile bool* volatile interrupted;

    bool readFromSocket(int socketDescriptor);

    bool writeToSocket(int socketDescriptor);

    bool readInt(int socketDescriptor, uint32_t *val);

    bool writeInt(int socketDescriptor, uint32_t *val);

    bool bufferAll(int socketDescriptor, uint8_t *buffer, unsigned long nBytes, bool isRead);

    bool readAll(int socketDescriptor, uint8_t *buffer, unsigned long nBytes);

    bool writeAll(int socketDescriptor, uint8_t *buffer, unsigned long nBytes);

    static uint8_t* initCharArray(uint32_t size);
};


#endif //CLIENT_MESSAGE_H
