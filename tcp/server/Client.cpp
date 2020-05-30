#include "Client.h"

#include <iostream>
#include <utility>
#include "Server.h"
#include "../ioutils.h"

namespace {
    class MessageWriteTask : public Runnable {
    public:
        MessageWriteTask(int socketFD, const Message& message, std::weak_ptr<Client> client)
                : m_socketFD(socketFD), m_message(message), m_client(std::move(client)) {}

        void run() override {
            if (writeFullBuffer(m_socketFD, m_message.data(), m_message.size()) != 0) {
                if (auto ptr = m_client.lock()) {
                    if (!ptr->stopped()) {
                        std::cerr << "Error while writing to client" << std::endl;
                        ptr->shutdown();
                    }
                }
            }
        }

    private:
        int m_socketFD;
        Message m_message;
        std::weak_ptr<Client> m_client;
    };
}

Client::Client(int socketFD, Server* server)
        : m_socketFD(socketFD), m_server(server), m_thread(&Client::run, this) {}

void Client::send(const Message& message, const std::shared_ptr<Client>& client) {
    m_pool.execute(std::make_shared<MessageWriteTask>(m_socketFD, message, client));
}

void Client::run() {
    char length_buffer[4];

    while (!m_stop) {
        int ret;
        if ((ret = readFullBuffer(m_socketFD, length_buffer, 4)) == 0) {
            auto length = intFromArray<uint32_t>(length_buffer);
            Message message(length);
            if ((ret = readFullBuffer(m_socketFD, message.writeDst(), length)) == 0) {
                m_server->newMessage(message);
                continue;
            }
        }
        if (m_stop) {
            return;
        }
        if (ret == 1) {
            std::cerr << "Client disconnected." << std::endl;
        } else {
            std::cerr << "Error while reading from client." << std::endl;
        }
        shutdown();
        return;
    }
}

Client::~Client() {
    m_stop = true;
    ::shutdown(m_socketFD, SHUT_RDWR);
    close(m_socketFD);
    if (std::this_thread::get_id() != m_thread.get_id()) {
        m_thread.join();
    } else {
        m_thread.detach();
    }
}

void Client::shutdown() {
    m_server->removeClient(this);
}

int Client::socketFD() const {
    return m_socketFD;
}

bool Client::stopped() const {
    return m_stop;
}
