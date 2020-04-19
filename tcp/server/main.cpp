#include <cstdio>
#include <cstdlib>
#include <iostream>

#include <netinet/in.h>
#include <unistd.h>

#include <cstring>
#include <utility>
#include <vector>
#include <queue>
#include <shared_mutex>
#include <mutex>
#include <thread>
#include <algorithm>
#include <atomic>
#include <csignal>

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
    std::shared_mutex chatHistoryMutex = std::shared_mutex();
    static Server buildServer(uint16_t portno, uint32_t clientsN);
    void run();
    void receiveMessage(const Message& message);
    void notifyClientIsDead(uint32_t clientId);
    uint32_t chatSize();
    void shutdown();
    const Message& messageAt(uint32_t index);
    ~Server();

private:
    std::atomic_bool isFinished = false;
    const int sockfd;
    std::shared_mutex clientsMutex;
    std::vector<ClientConnection *> clients;
    std::vector<std::thread *> threads;
    std::vector<Message> chatHistory;
    std::condition_variable_any chatHistoryUpdated;

    explicit Server(int sockfd);

    void messageDealing();
    void notifyClientIsDeadUnsafe(uint32_t clientId);
};

class ClientConnection {
public:
    const std::atomic_uint32_t clientId;

    explicit ClientConnection(int sockfd, uint32_t clientId, Server * server);
    void sendRoutine();
    void run();
    std::condition_variable_any shouldSendMessage = std::condition_variable_any();
    void shutdown();
    ~ClientConnection();

private:
    int sockfd;
    Server * server;
    std::string userName;
    std::atomic_bool isFinished = false;
    uint32_t nextSend = 0;

    std::queue<char> untreatedInput;
    std::string getName();
    Message readMessage();
    std::string readString();
    uint32_t readUInt32();
    void readSomething();
    void sendMessage(const Message& message) const;
};

void Message::setTime(uint16_t newTime) {
    time = newTime;
}

void ClientConnection::shutdown() {
    isFinished = true;
    if (::shutdown(sockfd, SHUT_WR) == 0) {
    }
    shouldSendMessage.notify_one();
}

void ClientConnection::sendRoutine() {
    std::shared_lock chatLock(server->chatHistoryMutex);
    while (true) {
        while (server->chatSize() == nextSend && !isFinished) {
            shouldSendMessage.wait(chatLock);
        }
        if (isFinished) {
            break;
        }
        while (nextSend < server->chatSize()) {
            try {
                sendMessage(server->messageAt(nextSend));
            } catch (ClientDisconnectedException &e) {
                return;
            }
            ++nextSend;
        }
    }
}

ClientConnection::~ClientConnection() {
    close(sockfd);
}

void ClientConnection::run() {
    try {
        userName = getName();
    } catch (ClientDisconnectedException &e) {
        shutdown();

        server->notifyClientIsDead(clientId);
        return;
    }

    {
        std::thread sendThread([&] { sendRoutine(); });
        while (!isFinished) {
            try {
                Message message = readMessage();
                server->receiveMessage(message);
            } catch (ClientDisconnectedException &e) {
                shutdown();
            }
        }
        sendThread.join();
    }
    server->notifyClientIsDead(clientId);
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
    while (untreatedInput.size() < length) {
        readSomething();
    }
    std::string result;
    for (uint32_t i = 0; i < length; ++i) {
        result += untreatedInput.front();
        untreatedInput.pop();
    }

    return result;
}

uint32_t ClientConnection::readUInt32() {
    while (untreatedInput.size() < sizeof(uint32_t)) {
        readSomething();
    }
    char buffer[sizeof(uint32_t)];
    for (char& c : buffer) {
        c = untreatedInput.front();
        untreatedInput.pop();
    }
    uint32_t result;
    memcpy(&result, buffer, sizeof(uint32_t));
    return ntohl(result);
}

void ClientConnection::readSomething() {
    static const uint32_t BUFFER_SIZE = 256;
    char buffer[BUFFER_SIZE];
    int received = read(sockfd, buffer, BUFFER_SIZE - 1);
    if (received <= 0) {
        throw ClientDisconnectedException();
    }
    for (int i = 0; i < received; ++i) {
        untreatedInput.push(buffer[i]);
    }
}

Message::Message(std::string senderName, std::string message, uint16_t time) : time(time),
                                                                               senderName(std::move(senderName)),
                                                                               message(std::move(message)) {}

std::vector<char> Message::toCharArray() const {
    uint32_t packageLength = sizeof(uint16_t) +
            sizeof(uint32_t) + message.size() +
            sizeof(uint32_t) + senderName.size();

    char buffer[packageLength];

    char * dataPointer = buffer;

    uint16_t netTime = htons(time);
    memcpy(dataPointer, (char *) &netTime, sizeof(uint16_t));
    dataPointer += sizeof(uint16_t);

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
                                                                                     server(server) {}

void ClientConnection::sendMessage(const Message &message) const {
    std::vector<char> bytes = message.toCharArray();
    uint32_t needSend = bytes.size();
    uint32_t alreadySent = 0;
    while (alreadySent != needSend) {
        int send = write(sockfd, bytes.data() + alreadySent, needSend - alreadySent);
        if (send <= 0) {
            throw ClientDisconnectedException();
        }
        alreadySent += send;
    }
}

// Must be called only inside read mutex
uint32_t Server::chatSize() {
    return chatHistory.size();
}

// Must be called only inside read mutex
const Message& Server::messageAt(uint32_t index) {
    return chatHistory[index];
}

void Server::messageDealing() {
    std::shared_lock lock(chatHistoryMutex);
    uint32_t lastSize = 0;
    while (true) {
        while (lastSize == chatSize() && !isFinished) {
            chatHistoryUpdated.wait(lock);
        }
        if (isFinished) {
            break;
        }
        std::shared_lock clientsLock(clientsMutex);
        for (ClientConnection * client : clients) {
            client->shouldSendMessage.notify_one();
        }
        lastSize = chatSize();
    }
}

void Server::notifyClientIsDead(uint32_t clientId) {
    std::unique_lock lock(clientsMutex);
    notifyClientIsDeadUnsafe(clientId);
}

void Server::notifyClientIsDeadUnsafe(uint32_t clientId) {
    auto deletePosition = std::find_if(clients.begin(), clients.end(), [&](ClientConnection * client) {
        return client->clientId == clientId;
    });
    if (deletePosition != clients.end()) {
        ClientConnection * pointer = *deletePosition;
        clients.erase(deletePosition);
        delete pointer;
    }
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

    return Server(sockfd);
}

Server::~Server() {
    close(sockfd);
}

Server::Server(int sockfd) : sockfd(sockfd) {}

void safeShutdownSocket(int socketfd) {
    if (shutdown(socketfd, SHUT_WR) == 0) {
        char buffer[256];
        while (read(socketfd, buffer, 255) > 0);
    }
}

void Server::run() {
    std::thread dealMessage([&] { messageDealing(); });
    sockaddr_in cli_addr {};
    uint32_t clilen = sizeof(cli_addr);
    uint32_t clientId = 0;

    while (true) {
        int newsockfd = accept(sockfd, (sockaddr *) &cli_addr, &clilen);

        if (newsockfd < 0) {
            if (isFinished) {
                break;
            } else {
                std::cerr << "Something gone wrong while accept" << std::endl;
                continue;
            }
        }

        std::unique_lock lock(clientsMutex);
        auto client = new ClientConnection(newsockfd, ++clientId, this);
        if (!isFinished) {
            clients.push_back(client);
            threads.push_back(new std::thread(&ClientConnection::run, client));
        } else {
            safeShutdownSocket(newsockfd);
            delete client;
            break;
        }
    }
    dealMessage.join();
    for (std::thread * thread : threads) {
        thread->join();
        delete thread;
    }
}

void Server::shutdown() {
    std::unique_lock lock(clientsMutex);
    ::shutdown(sockfd, SHUT_RDWR);
    isFinished = true;
    chatHistoryUpdated.notify_one();
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
    {
        std::unique_lock lock(chatHistoryMutex);
        Message newMessage = message;
        newMessage.setTime(getCurrentTime());
        chatHistory.push_back(newMessage);
    }
    chatHistoryUpdated.notify_one();
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
        if (s == "exit") {
            server.shutdown();
            serverThread.join();
            break;
        }
    }

    return 0;
}
