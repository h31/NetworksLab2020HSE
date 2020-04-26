#include "Client.h"

#include <iostream>
#include "Server.h"
#include "../ioutils.h"


Client::Client(int socketFD, Server* server) : m_socketFD(socketFD), m_server(server) {}

void Client::send(const Message& message) {
    m_messages.push(message);
}

Client::~Client() {
    ::shutdown(m_socketFD, SHUT_RDWR);
    close(m_socketFD);
}

void Client::shutdown() {
    m_server->removeClient(this);
}

int Client::socketFD() const {
    return m_socketFD;
}

void Client::write() {
    if (!m_messages.empty()) {
        const auto& message = m_messages.front();
        int rv = ::send(m_socketFD, message.data() + m_writePosition, message.size() - m_writePosition, 0);
        if (rv == 0) {
            shutdown();
            return;
        }
        if (rv > 0) {
            m_writePosition += rv;
        }
        if (m_writePosition == message.size()) {
            m_messages.pop();
            m_writePosition = 0;
        }
    }
    if (m_messages.empty()) {
        m_server->unregisterWrite(m_socketFD);
    }
}

void Client::read() {
    static const std::size_t PrefixSize = 4;
    int rv = 0;
    if (m_readPosition < PrefixSize) {
        rv = recv(m_socketFD, m_sizeBuffer + m_readPosition, PrefixSize - m_readPosition, 0);
    } else {
        if (m_readBuffer.empty()) {
            auto length = intFromArray<uint32_t>(m_sizeBuffer);
            m_readBuffer.emplace_back(length);
        }
        auto& buffer = m_readBuffer[0];
        rv = recv(m_socketFD, buffer.writeDst() + m_readPosition - PrefixSize,
                  buffer.size() - m_readPosition - PrefixSize, 0);
    }
    if (rv == 0) {
        shutdown();
        return;
    }
    if (rv > 0) {
        m_readPosition += rv;
    }
    if (!m_readBuffer.empty() && m_readPosition + PrefixSize == m_readBuffer[0].size()) {
        m_server->newMessage(m_readBuffer[0]);
        m_readBuffer.clear();
        m_readPosition = 0;
    }
}
