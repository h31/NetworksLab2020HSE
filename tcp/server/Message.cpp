//
// Created by ralsei on 20.04.2020.
//

#include "Message.h"

bool Message::readFromSocket(int socketDescriptor, int low) {
    int diff = 0;

    if (currentPosition < positions[low]) {
        int shift = currentPosition;
        if (low > 0) {
            shift -= positions[low - 1];
        }

        diff = readInt(socketDescriptor, &receivedLength, shift);
    } else if (currentPosition < positions[low + 1]) {
        int shift = currentPosition - positions[low];

        diff = readInt(socketDescriptor, &userNicknameLength, shift);

        if (currentPosition + diff == positions[low + 1]) {
            messageLength = receivedLength - userNicknameLength;
            userNickname = initCharArray(userNicknameLength);

            positions[low + 2] = positions[low + 1] + userNicknameLength;
            positions[low + 3] = positions[low + 2] + messageLength;
        }
    } else if (currentPosition < positions[low + 2]) {
        int left = positions[low + 2] - currentPosition;

        diff = read(socketDescriptor, userNickname, left);

        if (currentPosition + diff == positions[low + 2]) {
            message = initCharArray(messageLength);
        }
    } else if (currentPosition < positions[low + 3]) {
        int left = positions[low + 3] - currentPosition;

        diff = read(socketDescriptor, message, left);
    }

    currentPosition += diff;
    return diff < 0;
}

void Message::initReadFromClientSocket() {
    numberOfPositions = 4;
    positions = new int32_t[numberOfPositions];
    positions[0] = 4;
    positions[1] = positions[0] + 4;
    init = true;
}

bool Message::readFromClientSocket(int socketDescriptor) {
    if (!init) {
        initReadFromClientSocket();
    }

    bool result = readFromSocket(socketDescriptor, 0);

    if (fullyRead()) {
        auto currentTime = std::time(nullptr);
        auto localTime = localtime(&currentTime);
        time = (localTime->tm_hour * 60 + localTime->tm_min) * 60 + localTime->tm_sec;

        return false;
    }

    return result;
}

bool Message::fullyRead() {
    if (positions == nullptr) {
        return false;
    }

    return currentPosition == positions[numberOfPositions - 1];
}

void Message::initReadFromServer() {
    numberOfPositions = 5;
    positions = new int32_t[numberOfPositions];
    positions[0] = 4;
    positions[1] = positions[0] + 4;
    positions[2] = positions[1] + 4;
    positions[3] = positions[2] + userNicknameLength;
    positions[4] = positions[3] + messageLength;

    init = true;
}

bool Message::readFromServer(int socketDescriptor) {
    if (!init) {
        initReadFromServer();
    }

    int diff = 0;

    if (currentPosition < positions[0]) {
        diff = readInt(socketDescriptor, &time, currentPosition);
        currentPosition += diff;
    } else if (currentPosition < positions[1]) {
        diff = readFromSocket(socketDescriptor, 1);
    }

    return diff < 0;
}

bool Message::writeToSocket(int socketDescriptor, int low) {
    uint32_t sendLength = userNicknameLength + messageLength;

    int diff = 0;

    if (currentPosition < positions[low]) {
        int shift = currentPosition;
        if (low > 0) {
            shift -= positions[low - 1];
        }

        diff = writeInt(socketDescriptor, &sendLength, shift);
    } else if (currentPosition < positions[low + 1]) {
        int shift = currentPosition - positions[low];

        diff = writeInt(socketDescriptor, &userNicknameLength, shift);
    } else if (currentPosition < positions[low + 2]) {
        int left = positions[low + 2] - currentPosition;

        diff = write(socketDescriptor, userNickname, left);
    } else if (currentPosition < positions[low + 3]) {
        int left = positions[low + 3] - currentPosition;

        diff = write(socketDescriptor, message, left);
    }

    currentPosition += diff;
    return diff < 0;
}

void Message::initWriteToClientSocket() {
    numberOfPositions = 5;
    positions = new int32_t[numberOfPositions];
    positions[0] = 4;
    positions[1] = positions[0] + 4;
    positions[2] = positions[1] + 4;
    positions[3] = positions[2] + userNicknameLength;
    positions[4] = positions[3] + messageLength;

    init = true;
}

bool Message::writeToClientSocket(int socketDescriptor) {
    if (!init) {
        initWriteToClientSocket();
    }

    int diff = 0;

    if (currentPosition < positions[0]) {
        diff = writeInt(socketDescriptor, &time, currentPosition);
        currentPosition += diff;
    } else {
        diff = writeToSocket(socketDescriptor, 1);
    }

    return diff < 0;
}

void Message::initWriteToServerSocket() {
    numberOfPositions = 4;
    positions = new int32_t[numberOfPositions];
    positions[0] = 4;
    positions[1] = positions[0] + 4;
    positions[2] = positions[1] + userNicknameLength;
    positions[3] = positions[2] + messageLength;

    init = true;
}

bool Message::writeToServerSocket(int socketDescriptor) {
    if (!init) {
        initWriteToServerSocket();
    }

    return writeToSocket(socketDescriptor, 0);
}

int Message::readOrWrite(int socketDescriptor, uint8_t *buffer, unsigned long nBytes, bool isRead) {
    int diff;

    if (isRead) {
        diff = recv(socketDescriptor, buffer, nBytes, 0);
    } else {
        diff = send(socketDescriptor, buffer, nBytes, 0);
    }

    return diff;
}

int Message::read(int socketDescriptor, uint8_t *buffer, unsigned long nBytes) {
    return readOrWrite(socketDescriptor, buffer, nBytes, true);
}

int Message::write(int socketDescriptor, uint8_t *buffer, unsigned long nBytes) {
    return readOrWrite(socketDescriptor, buffer, nBytes, false);
}

int Message::writeInt(int socketDescriptor, uint32_t *val, int shift) {
    uint32_t bytesInInt = 4;
    uint32_t bitsInByte = 8;

    if (shift == 0) {
        currentIntBytes = initCharArray(bytesInInt);
        for (uint32_t i = 0; i < bytesInInt; i++) {
            currentIntBytes[bytesInInt - i - 1] = (((*val) >> (bitsInByte * i)) & 0xFFu);
        }
    }

    return write(socketDescriptor, currentIntBytes + shift, bytesInInt - shift);
}

int Message::readInt(int socketDescriptor, uint32_t *val, int shift) {
    uint32_t bytesInInt = 4;
    uint32_t bitsInByte = 8;

    if (shift == 0) {
        *val = 0;
        currentIntBytes = initCharArray(bytesInInt);
    }

    auto diff = read(socketDescriptor, currentIntBytes + shift, bytesInInt - shift);

    if (diff + shift == bytesInInt) {
        for (uint32_t i = 0; i < bytesInInt; i++) {
            *val |= ((uint32_t) ((uint32_t) (currentIntBytes[bytesInInt - i - 1u]) << (bitsInByte * i)));
        }

        delete currentIntBytes;
    }

    return diff;
}

Message::~Message() {
    delete userNickname;
    delete message;
}

void Message::printMessage() {
    std::cout << "[" << userNickname << "] ";

    int m_time = time;
    int hours = m_time / 3600;
    m_time -= hours * 3600;
    int minutes = m_time / 60;
    m_time -= minutes * 60;
    int seconds = m_time;

    std::cout << "<" << hours << ":" << minutes << ":" << seconds << "> ";

    std::cout << message << std::endl;
}

uint8_t *Message::initCharArray(uint32_t size) {
    auto result = new uint8_t[size];
    for (uint32_t i = 0; i < size; i++) {
        result[i] = 0;
    }

    return result;
}

Message::Message(uint32_t userNicknameLength, uint8_t *userNickname, uint32_t messageLength, uint8_t *message) {
    this->messageLength = messageLength;
    this->userNicknameLength = userNicknameLength;
    this->userNickname = new uint8_t[userNicknameLength];
    for (uint32_t i = 0; i < userNicknameLength; i++) {
        this->userNickname[i] = userNickname[i];
    }

    this->message = new uint8_t[messageLength];
    for (uint32_t i = 0; i < messageLength; i++) {
        this->message[i] = message[i];
    }
}

Message::Message(Message *other) : Message(other->userNicknameLength, other->userNickname, other->messageLength,
                                           other->message) {
    this->time = other->time;
}
