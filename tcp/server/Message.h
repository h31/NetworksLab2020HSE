//
// Created by ralsei on 20.04.2020.
//

#ifndef SERVER_MESSAGE_H
#define SERVER_MESSAGE_H

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
    Message() = default;

    explicit Message(Message* other);

    Message(uint32_t userNicknameLength, uint8_t *userNickname, uint32_t messageLength, uint8_t *message);

    ~Message();

    bool readFromClientSocket(int socketDescriptor);

    bool readFromServer(int socketDescriptor);

    bool writeToClientSocket(int socketDescriptor);

    bool writeToServerSocket(int socketDescriptor);

    void printMessage();

    bool fullyRead();

private:
    bool init = false;

    uint32_t userNicknameLength = 0;
    uint8_t *userNickname = nullptr;
    uint32_t messageLength = 0;
    uint8_t *message = nullptr;
    uint32_t time = -1;

    int32_t currentPosition = 0;
    int32_t numberOfPositions = 0;
    int32_t* positions = nullptr;
    uint8_t* currentIntBytes = nullptr;


    bool readFromSocket(int socketDescriptor, int low);

    bool writeToSocket(int socketDescriptor, int low);

    int readInt(int socketDescriptor, uint32_t *val, int shift);

    int writeInt(int socketDescriptor, uint32_t *val, int shift);

    int readOrWrite(int socketDescriptor, uint8_t *buffer, unsigned long nBytes, bool isRead);

    int read(int socketDescriptor, uint8_t *buffer, unsigned long nBytes);

    int write(int socketDescriptor, uint8_t *buffer, unsigned long nBytes);

    static uint8_t* initCharArray(uint32_t size);

    void initReadFromClientSocket();

    void initReadFromServer();

    void initWriteToClientSocket();

    void initWriteToServerSocket();
};


#endif //SERVER_MESSAGE_H
