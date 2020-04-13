#include "Client.h"

#include <iostream>
#include "Server.h"
#include "../ioutils.h"

namespace {
    class MessageWriteTask : public Runnable {
    public:
        MessageWriteTask(int socketFD, const Message& message) : m_socketFD(socketFD), m_message(message) {}

        void run() override {
            if (writeFullBuffer(m_socketFD, m_message.data(), m_message.size()) != 0) {
                std::cerr << "Error while writing to client" << std::endl;
            }
        }

    private:
        int m_socketFD;
        Message m_message;
    };
}

Client::Client(int socketFD, Server* server)
        : m_socketFD(socketFD), m_server(server), m_thread(&Client::run, this) {}

void Client::send(const Message& message) {
    m_pool.execute(std::make_shared<MessageWriteTask>(m_socketFD, message));
}

void Client::run() {
    char length_buffer[4];

    while (true) {
        if (readFullBuffer(m_socketFD, length_buffer, 4) == 0) {
            auto length = intFromArray<uint32_t, 4>(length_buffer);
            Message message(length);
            if (readFullBuffer(m_socketFD, message.writeDst(), length) == 0) {
                m_server->newMessage(message);
                continue;
            }
        }
        std::cerr << "Error while reading from client." << std::endl;
    }
}

Client::~Client() {
    close(m_socketFD);
}
