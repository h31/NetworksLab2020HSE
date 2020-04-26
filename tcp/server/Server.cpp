#include <sys/socket.h>
#include <netinet/in.h>
#include <iostream>
#include <algorithm>
#include <asm/ioctls.h>
#include <stropts.h>
#include "Server.h"


Server::Server(uint16_t port, int maxClientsNumber) {
    m_sockFD = socket(AF_INET, SOCK_STREAM, 0);
    if (m_sockFD < 0) {
        perror("ERROR opening socket");
        exit(1);
    }
    int on = 1;
    if (ioctl(m_sockFD, FIONBIO, (char*) &on)) {
        perror("ioctl() failed");
        close(m_sockFD);
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
    registerRead(m_sockFD);
    m_thread = std::thread(&Server::run, this);
}

void Server::run() {
    while (!m_stop) {
        if (poll(m_fds.data(), m_fds.size(), -1) < 0) {
            perror("poll failed");
            break;
        }

        const auto readyRead = readyReadClients();
        const auto readyWrite = readyWriteClient();
        if (m_fds[0].revents & POLLIN) {
            acceptClients();
        }
        for (const auto& client : readyRead) {
            client->read();
        }
        for (const auto& client : readyWrite) {
            client->write();
        }
    }
}


void Server::newMessage(const Message& message) {
    for (auto& fd : m_fds) {
        if (fd.fd == m_sockFD) continue;
        fd.events |= POLLOUT;
        m_clients[fd.fd]->send(message);
    }
}

Server::~Server() {
    m_stop = true;
    shutdown(m_sockFD, SHUT_RDWR);
    close(m_sockFD);
    m_thread.join();
}

void Server::removeClient(const Client* client) {
    const auto fd = client->socketFD();
    auto it = std::find_if(m_fds.begin(), m_fds.end(), [&](const auto& a) -> bool { return a.fd == fd; });
    if (it != m_fds.end()) {
        m_clients.erase(fd);
        std::cout << "Remove client " << fd << std::endl;
        m_fds.erase(it);
    }
}

void Server::acceptClients() {
    while (true) {
        sockaddr_in newAddress{};
        auto* newAddressPtr = reinterpret_cast<sockaddr*>(&newAddress);
        auto addressLength = sizeof(m_serverAddress);
        int newSocketFD = accept(m_sockFD, newAddressPtr, reinterpret_cast<socklen_t*>(&addressLength));
        if (m_stop || newSocketFD < 0) {
            return;
        }
        std::cout << "A new client accepted " << newSocketFD << std::endl;
        auto client = std::make_shared<Client>(newSocketFD, this);
        registerRead(newSocketFD);
        m_clients[newSocketFD] = client;
    }
}

void Server::unregisterWrite(int socketFD) {
    for (auto& fd : m_fds) {
        if (fd.fd == socketFD) {
            fd.events ^= POLLOUT;
        }
    }
}

std::vector<std::shared_ptr<Client>> Server::readyReadClients() {
    std::vector<std::shared_ptr<Client>> result;
    for (const auto& request : m_fds) {
        if (request.fd != m_sockFD && request.revents & POLLIN) {
            result.push_back(m_clients[request.fd]);
        }
    }
    return result;
}

std::vector<std::shared_ptr<Client>> Server::readyWriteClient() {
    std::vector<std::shared_ptr<Client>> result;
    for (const auto& request : m_fds) {
        if (request.revents & POLLOUT) {
            result.push_back(m_clients[request.fd]);
        }
    }
    return result;
}

void Server::registerRead(int socketFD) {
    pollfd fd{};
    fd.fd = socketFD;
    fd.events = POLLIN;
    m_fds.push_back(fd);
}

