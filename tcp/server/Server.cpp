#include <sys/socket.h>
#include <netinet/in.h>
#include <iostream>
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
        std::cout << "A new client accepted" << std::endl;
        auto client = std::make_shared<Client>(newSocketFD, this);
        m_clients.push_back(client);
    }
}

void Server::newMessage(const Message& message) {
    for (auto& client : m_clients) {
        client->send(message);
    }
}

Server::~Server() {
    close(m_sockFD);
}

