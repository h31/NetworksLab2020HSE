#include <sys/socket.h>
#include <netinet/in.h>
#include <iostream>
#include <algorithm>
#include "Server.h"


Server::Server(uint16_t port, int maxClientsNumber) {
    m_sockFD = socket(AF_INET, SOCK_STREAM, 0);
    if (m_sockFD < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    bzero(&m_serverAddress, sizeof(m_serverAddress));
    m_serverAddress.sin_family = AF_INET;
    m_serverAddress.sin_addr.s_addr = INADDR_ANY;
    m_serverAddress.sin_port = htons(port);
    const auto* address = reinterpret_cast<const sockaddr*>(&m_serverAddress);

    if (bind(m_sockFD, address, sizeof(m_serverAddress)) < 0) {
        perror("ERROR binding");
        exit(1);
    }

    listen(m_sockFD, maxClientsNumber);
}

void Server::run() {
    while (true) {
        sockaddr_in newAddress{};
        auto* newAddressPtr = reinterpret_cast<sockaddr*>(&newAddress);
        auto addressLength = sizeof(m_serverAddress);
        int newSocketFD = accept(m_sockFD, newAddressPtr, reinterpret_cast<socklen_t*>(&addressLength));
        if (newSocketFD < 0) {
            std::cerr << "ERROR accepting client" << std::endl;
            continue;
        }
        std::cout << "A new client accepted " << newSocketFD << std::endl;
        auto client = std::make_shared<Client>(newSocketFD, this);
        m_mutex.lock();
        m_clients.push_back(client);
        m_mutex.unlock();
    }
}

void Server::newMessage(const Message& message) {
    m_mutex.lock();
    for (auto& client : m_clients) {
        client->send(message, client);
    }
    m_mutex.unlock();
}

Server::~Server() {
    close(m_sockFD);
}

void Server::removeClient(const Client* client) {
    m_mutex.lock();
    auto it = std::find_if(m_clients.begin(), m_clients.end(),
                           [&](const std::shared_ptr<Client>& a) { return a.get() == client; });
    if (it != m_clients.end()) {
        std::cout << "Remove client " << client->socketFD() << std::endl;
        m_clients.erase(it);
    }
    m_mutex.unlock();
}

