//
// Created by ralsei on 20.04.2020.
//

#include "Message.h"

bool Message::readFromSocket(int socketDescriptor) {
    uint32_t receivedLength;
    bool failed = readInt(socketDescriptor, &receivedLength);
    if (failed) {
        return true;
    }

    failed = readInt(socketDescriptor, &userNicknameLength);
    if (failed) {
        return true;
    }

    messageLength = receivedLength - userNicknameLength;

    userNickname = initCharArray(userNicknameLength);
    failed = readAll(socketDescriptor, userNickname, userNicknameLength);
    if (failed) {
        return true;
    }

    message = initCharArray(messageLength);
    failed = readAll(socketDescriptor, message, messageLength);
    return failed;
}

bool Message::readFromClientSocket(int socketDescriptor) {
    readFromSocket(socketDescriptor);

    auto currentTime = std::time(nullptr);
    auto localTime = localtime(&currentTime);
    time = (localTime->tm_hour * 60 + localTime->tm_min) * 60 + localTime->tm_sec;

    return false;
}

bool Message::readFromServer(int socketDescriptor) {
    bool failed = readInt(socketDescriptor, &time);
    if (failed) {
        return true;
    }

    failed = readFromSocket(socketDescriptor);
    return failed;
}

bool Message::writeToSocket(int socketDescriptor) {
    uint32_t sendLength = userNicknameLength + messageLength;
    bool failed = writeInt(socketDescriptor, &sendLength);
    if (failed) {
        return true;
    }

    failed = writeInt(socketDescriptor, &userNicknameLength);
    if (failed) {
        return true;
    }

    failed = writeAll(socketDescriptor, userNickname, userNicknameLength);
    if (failed) {
        return true;
    }

    failed = writeAll(socketDescriptor, message, messageLength);
    return failed;

}

bool Message::writeToClientSocket(int socketDescriptor) {
    bool failed = writeInt(socketDescriptor, &time);
    if (failed) {
        return true;
    }

    return writeToSocket(socketDescriptor);
}

bool Message::writeToServerSocket(int socketDescriptor) {
    return writeToSocket(socketDescriptor);
}

bool Message::bufferAll(int socketDescriptor, uint8_t *buffer, unsigned long nBytes, bool isRead) {
    for (uint32_t i = 0; nBytes > 0;) {
        int diff;

        if (isRead) {
            diff = read(socketDescriptor, buffer + i, nBytes);
        } else {
            diff = write(socketDescriptor, buffer + i, nBytes);
        }

        if ((*interrupted) || diff < 0) {
            return true;
        }

        i += diff;
        nBytes -= diff;
    }

    return false;
}

bool Message::readAll(int socketDescriptor, uint8_t *buffer, unsigned long nBytes) {
    return bufferAll(socketDescriptor, buffer, nBytes, true);
}

bool Message::writeAll(int socketDescriptor, uint8_t *buffer, unsigned long nBytes) {
    return bufferAll(socketDescriptor, buffer, nBytes, false);
}

bool Message::writeInt(int socketDescriptor, uint32_t *val) {
    uint32_t bytesInInt = 4;
    uint32_t bitsInByte = 8;

    auto* bytes = initCharArray(bytesInInt);
    for (uint32_t i = 0; i < bytesInInt; i++) {
        bytes[bytesInInt - i - 1] = (((*val) >> (bitsInByte * i)) & 0xFFu);
    }

    bool failed = writeAll(socketDescriptor, bytes, bytesInInt);
    return failed;
}

bool Message::readInt(int socketDescriptor, uint32_t *val) {
    *val = 0;

    uint32_t bytesInInt = 4;
    uint32_t bitsInByte = 8;

    auto* bytes = initCharArray(bytesInInt);

    bool failed = readAll(socketDescriptor, bytes, bytesInInt);
    if (!failed) {
        for (uint32_t i = 0; i < bytesInInt; i++) {
            *val |= ((uint32_t) ((uint32_t) (bytes[bytesInInt - i - 1u]) << (bitsInByte * i)));
        }
    }

    return failed;
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

uint8_t* Message::initCharArray(uint32_t size) {
    auto result = new uint8_t[size];
    for (uint32_t i = 0; i < size; i++) {
        result[i] = 0;
    }

    return result;
}