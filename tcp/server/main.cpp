#include <cstdio>
#include <cstdlib>
#include <iostream>

#include <netinet/in.h>
#include <unistd.h>

#include <cstring>
#include <utility>
#include <vector>
#include <queue>
#include <mutex>
#include <thread>
#include <algorithm>
#include <atomic>
#include <csignal>
#include <fcntl.h>
#include <poll.h>
#include <cassert>

class ClientDisconnectedException : public std::exception {};

class Message {
public:
    Message(std::string senderName, std::string message, uint16_t time = 0);
    [[nodiscard]] std::vector<char> toCharArray() const;
    void setTime(uint16_t newTime);

    uint16_t time;
    std::string senderName;
    std::string message;
};

class ClientConnection;

class Server {
public:
    static Server buildServer(uint16_t portno, uint32_t clientsN);
    void run();
    void receiveMessage(const Message& message);
    uint32_t chatSize();
    void shutdown();
    const Message& messageAt(uint32_t index);
    ~Server();

private:
    std::atomic_bool isFinished = false;
    const int sockfd;
    std::vector<ClientConnection *> clients;
    std::vector<Message> chatHistory;

    explicit Server(int sockfd);

    void accept();
    void deleteDisconnectedClients(std::vector<uint32_t> disconnectedClients);
};

class ClientConnection {
public:
    const std::atomic_uint32_t clientId;
    const int sockfd;

    explicit ClientConnection(int sockfd, uint32_t clientId, Server * server);
    void run();
    [[nodiscard]] bool haveToSendMessage() const;
    void readSomething();
    void writeSomething();
    void shutdown();
    void addNewMessages();
    void tryReadMessages();
    ~ClientConnection();

private:
    enum MessageState {
        INTRODUCE_MESSAGE,
        ROUTINE
    };

    MessageState state = INTRODUCE_MESSAGE;
    Server * server;
    std::string userName;
    uint32_t nextSend = 0;
    std::deque<char> sendQueue;


    std::deque<char> untreatedInput;
    std::string getName();
    Message readMessage();
    std::string readString();
    uint32_t readUInt32();
    [[nodiscard]] bool canReadNextMessage() const;
    void sendMessage(const Message& message);
};

void Message::setTime(uint16_t newTime) {
    time = newTime;
}

bool ClientConnection::haveToSendMessage() const {
    return !sendQueue.empty();
}

void ClientConnection::writeSomething() {
    static const uint32_t BUFFER_SIZE = 256;

    char buffer[BUFFER_SIZE];
    int send;

    do {
        uint32_t bufferPointer = 0;
        while (bufferPointer < std::min(BUFFER_SIZE, (uint32_t) sendQueue.size())) {
            buffer[bufferPointer] = sendQueue[bufferPointer];
            ++bufferPointer;
        }

        send = write(sockfd, buffer, bufferPointer);

        if (send < 0) {
            throw ClientDisconnectedException();
        }

        for (int i = 0; i < send; ++i) {
            sendQueue.pop_front();
        }

    } while (send != 0 && haveToSendMessage());
}

void ClientConnection::shutdown() {
    if (::shutdown(sockfd, SHUT_WR) == 0) {
    }
}

ClientConnection::~ClientConnection() {
    std::cerr << "Disconnected user " << clientId << std::endl;
    close(sockfd);
}

void ClientConnection::run() {
    std::cerr << "Connected user " << clientId << std::endl;
    addNewMessages();
}

std::string ClientConnection::getName() {
    return readString();
}

Message ClientConnection::readMessage() {
    std::string message = readString();
    return Message(userName, message);
}

std::string ClientConnection::readString() {
    uint32_t length = readUInt32();
    assert(untreatedInput.size() >= length);
    std::string result;
    for (uint32_t i = 0; i < length; ++i) {
        result += untreatedInput.front();
        untreatedInput.pop_front();
    }

    return result;
}

uint32_t ClientConnection::readUInt32() {
    assert(untreatedInput.size() >= sizeof(uint32_t));
    char buffer[sizeof(uint32_t)];
    for (char& c : buffer) {
        c = untreatedInput.front();
        untreatedInput.pop_front();
    }
    uint32_t result;
    memcpy(&result, buffer, sizeof(uint32_t));
    return ntohl(result);
}

void ClientConnection::readSomething() {
    static const uint32_t BUFFER_SIZE = 256;
    char buffer[BUFFER_SIZE];
    int received;
    do {
        received = read(sockfd, buffer, BUFFER_SIZE - 1);
        if (received == 0) {
            throw ClientDisconnectedException();
        }
        for (int i = 0; i < received; ++i) {
            untreatedInput.push_back(buffer[i]);
        }
    } while (received != -1);
}

Message::Message(std::string senderName, std::string message, uint16_t time) : time(time),
                                                                               senderName(std::move(senderName)),
                                                                               message(std::move(message)) {}

std::vector<char> Message::toCharArray() const {
    uint32_t packageLength = sizeof(uint32_t) + sizeof(uint16_t) +
            sizeof(uint32_t) + message.size() +
            sizeof(uint32_t) + senderName.size();

    char buffer[packageLength];

    char * dataPointer = buffer;

    {
        uint32_t bodyLength = packageLength - sizeof(uint32_t);
        uint32_t netBodyLength = htonl(bodyLength);
        memcpy(dataPointer, (char *) &netBodyLength, sizeof(uint32_t));
        dataPointer += sizeof(uint32_t);
    }

    {
        uint16_t netTime = htons(time);
        memcpy(dataPointer, (char *) &netTime, sizeof(uint16_t));
        dataPointer += sizeof(uint16_t);
    }

    {
        // message
        uint32_t messageLength = message.size();
        uint32_t netMessageLength = htonl(messageLength);
        memcpy(dataPointer, (char *) &netMessageLength, sizeof(uint32_t));
        dataPointer += sizeof(uint32_t);

        memcpy(dataPointer, message.c_str(), messageLength);
        dataPointer += messageLength;
    }

    {
        // sender name
        uint32_t senderNameLength = senderName.size();
        uint32_t netSenderNameLength = htonl(senderNameLength);
        memcpy(dataPointer, (char *) &netSenderNameLength, sizeof(uint32_t));
        dataPointer += sizeof(uint32_t);

        memcpy(dataPointer, senderName.c_str(), senderNameLength);
        dataPointer += senderNameLength;
    }

    return std::vector<char>(buffer, buffer + packageLength);
}

ClientConnection::ClientConnection(int sockfd, uint32_t clientId, Server * server) : clientId(clientId),
                                                                                     sockfd(sockfd),
                                                                                     server(server) {
    int nonBlockResult = fcntl(sockfd, F_SETFL, O_NONBLOCK);
    assert(nonBlockResult == 0);
}

void ClientConnection::sendMessage(const Message &message) {
    std::vector<char> bytes = message.toCharArray();
    for (char byte : bytes) {
        sendQueue.push_back(byte);
    }
}

void ClientConnection::addNewMessages() {
    while (nextSend < server->chatSize()) {
        sendMessage(server->messageAt(nextSend));
        ++nextSend;
    }
}

void ClientConnection::tryReadMessages() {
    while (canReadNextMessage()) {
        if (state == INTRODUCE_MESSAGE) {
            std::string name = getName();
            userName = name;
            state = ROUTINE;
        } else if (state == ROUTINE) {
            Message message = readMessage();
            server->receiveMessage(message);
        }
    }
}

bool ClientConnection::canReadNextMessage() const {
    if (untreatedInput.size() < sizeof(uint32_t)) {
        return false;
    }
    char buffer[sizeof(uint32_t)];
    for (uint32_t i = 0; i < sizeof(uint32_t); ++i) {
        buffer[i] = untreatedInput[i];
    }
    uint32_t length;
    memcpy(&length, buffer, sizeof(uint32_t));
    return untreatedInput.size() >= sizeof(uint32_t) + ntohl(length);
}

uint32_t Server::chatSize() {
    return chatHistory.size();
}

const Message& Server::messageAt(uint32_t index) {
    return chatHistory[index];
}

Server Server::buildServer(uint16_t portno, uint32_t clientsN) {
    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        std::cerr << "Error while building" << std::endl;
        throw std::exception();
    }
    int one = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(int));

    sockaddr_in serv_addr {};
    bzero((char *) &serv_addr, sizeof(serv_addr));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);

    if (bind(sockfd, (sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR on binding");
        throw std::exception();
    }

    if (listen(sockfd, clientsN) < 0) {
        throw std::exception();
    }

    fcntl(sockfd, F_SETFL, O_NONBLOCK);

    return Server(sockfd);
}

Server::~Server() {
    close(sockfd);
}

Server::Server(int sockfd) : sockfd(sockfd) {}

void safeShutdownSocket(int socketfd) {
    if (shutdown(socketfd, SHUT_WR) == 0) {
        char buffer[256];
        while (read(socketfd, buffer, 255) != 0);
    }
}

void Server::run() {
    while (!isFinished) {
        size_t pollsSize = clients.size() + 1;
        pollfd polls[pollsSize];
        polls[0] = pollfd { sockfd, POLLIN, 0 };

        uint32_t currentIndex = 0;
        for (ClientConnection * client : clients) {
            short mask = POLLIN;
            if (client->haveToSendMessage()) {
                mask |= POLLOUT;
            }
            polls[++currentIndex] = pollfd { client->sockfd, mask, 0 };
        }

        int pollResult = poll(polls, pollsSize, -1);
        if (pollResult < 0) {
            std::cerr << "Poll returned negative value.\n";
            throw 1;
        }

        if (polls[0].revents & POLLIN) {
            accept();
        }

        std::vector<uint32_t> disconnectedClients;

        for (uint32_t pollId = 1; pollId < pollsSize; ++pollId) {
            ClientConnection * client = clients[pollId - 1];
            short revents = polls[pollId].revents;

            try {
                if (revents & POLLIN) {
                    client->readSomething();
                }
                if (revents & POLLOUT) {
                    client->writeSomething();
                }
            } catch (ClientDisconnectedException& e) {
                disconnectedClients.push_back(pollId - 1);
            }
        }

        for (ClientConnection * client: clients) {
            client->tryReadMessages();
        }

        for (ClientConnection * client: clients) {
            client->addNewMessages();
        }

        deleteDisconnectedClients(disconnectedClients);
    }

    for (ClientConnection * client : clients) {
        safeShutdownSocket(client->sockfd);
        delete client;
    }
}

void Server::deleteDisconnectedClients(std::vector<uint32_t> disconnectedClients) {
    std::reverse(disconnectedClients.begin(), disconnectedClients.end());

    for (uint32_t disconnectedClientIndex : disconnectedClients) {
        ClientConnection * deletedClient = clients[disconnectedClientIndex];
        std::swap(clients[disconnectedClientIndex], clients.back());
        clients.pop_back();

        delete deletedClient;
    }
}

void Server::accept() {
    static sockaddr_in cli_addr {};
    static uint32_t clilen = sizeof(cli_addr);
    static uint32_t clientId = 0;

    int newsockfd = ::accept(sockfd, (sockaddr *) &cli_addr, &clilen);

    if (newsockfd < 0) {
        std::cerr << "Something gone wrong while accept" << std::endl;
        return;
    }

    auto client = new ClientConnection(newsockfd, ++clientId, this);
    clients.push_back(client);
    client->run();
}

void Server::shutdown() {
    ::shutdown(sockfd, SHUT_RDWR);
    isFinished = true;
    std::vector<ClientConnection *> clientsCopy = clients;
    for (ClientConnection * client : clientsCopy) {
        client->shutdown();
    }
}

uint16_t getCurrentTime() {
    std::time_t t = std::time(nullptr);
    std::tm* now = std::localtime(&t);
    return now->tm_hour * 60 + now->tm_min;
}

void Server::receiveMessage(const Message &message) {
    Message newMessage = message;
    newMessage.setTime(getCurrentTime());
    chatHistory.push_back(newMessage);
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    signal(SIGPIPE, SIG_IGN);

    uint16_t portno = atoi(argv[1]);

    Server server = Server::buildServer(portno, 100);
    std::thread serverThread([&] { server.run(); });
    std::string s;
    while (getline(std::cin, s)) {
        if (s == "/shutdown") {
            break;
        }
    }

    server.shutdown();
    serverThread.join();

    return 0;
}
