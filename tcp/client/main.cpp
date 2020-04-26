#include <cstdio>
#include <cstdlib>
#include <iostream>

#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>

#include <string>
#include <cstring>
#include <thread>
#include <utility>
#include <queue>
#include <csignal>
#include <fcntl.h>
#include <cassert>
#include <mutex>
#include <poll.h>

class ReadException : public std::exception {
};

class Message {
public:
    Message(std::string senderName, std::string message, uint16_t time);

    const uint16_t time;
    const std::string senderName;
    const std::string message;
};

Message::Message(std::string senderName, std::string message, uint16_t time) : time(time),
                                                                               senderName(std::move(senderName)),
                                                                               message(std::move(message)) {}


class Connection {
public:
    static Connection buildConnection(char * hostname, char * port);
    void addMessage(const std::string &message);

    void getMessages();
    void shutdown();
    ~Connection();

private:
    int sockfd;

    [[nodiscard]] bool hasSomethingToSend() const;
    [[nodiscard]] bool canReadMessage() const;

    std::deque<char> untreatedInput;
    std::deque<char> sendQueue;
    Message readMessage();
    uint32_t readUInt32();
    uint16_t readUInt16();
    std::string readString();
    void readSomething();
    explicit Connection(int sockfd);
    static int getSockfd(char * hostname, char * port);
    void tryReadMessage();
    void sendSomething();
};

Connection::~Connection() {
    close(sockfd);
}

Connection Connection::buildConnection(char * hostname, char * port) {
    int sockfd = getSockfd(hostname, port);

    int nonBlockResult = fcntl(sockfd, F_SETFL, O_NONBLOCK);
    assert(nonBlockResult == 0);

    return Connection(sockfd);
}

void Connection::shutdown() {
    ::shutdown(sockfd, SHUT_RDWR);
    char buffer[256];
    while (read(sockfd, buffer, 255) != 0);
}

void Connection::tryReadMessage() {
    if (canReadMessage()) {
        Message message = readMessage();
        char time[8];
        sprintf(time, "<%02d:%02d>", message.time / 60, message.time % 60);
        std::cout << std::string(time) << " [" << message.senderName << "] " << message.message << std::endl;
    }
}

void Connection::getMessages() {
    while (true) {
        pollfd polls[1];
        polls[0] = {sockfd, POLLOUT, 0};
        int pollResult = poll(polls, 1, -1);
        if (pollResult < 0) {
            std::cout << "Poll error" << std::endl;
            shutdown();
            break;
        }
        try {
            readSomething();
            tryReadMessage();
        } catch (const ReadException& e) {
            std::cout << "Disconnected from server" << std::endl;
            shutdown();
            break;
        }
    }
}

Message Connection::readMessage() {
    readUInt32();
    uint16_t time = readUInt16();
    std::string message = readString();
    std::string senderName = readString();
    return Message(senderName, message, time);
}

std::string Connection::readString() {
    uint32_t length = readUInt32();
    assert(untreatedInput.size() >= length);
    std::string result;
    for (uint32_t i = 0; i < length; ++i) {
        result += untreatedInput.front();
        untreatedInput.pop_front();
    }
    return result;
}

uint32_t Connection::readUInt32() {
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

uint16_t Connection::readUInt16() {
    assert(untreatedInput.size() >= sizeof(uint16_t));
    char buffer[sizeof(uint16_t)];
    for (char& c : buffer) {
        c = untreatedInput.front();
        untreatedInput.pop_front();
    }
    uint16_t result;
    memcpy(&result, buffer, sizeof(uint16_t));
    return ntohs(result);
}

void Connection::readSomething() {
    static const uint32_t BUFFER_SIZE = 256;
    char buffer[BUFFER_SIZE];
    int received = read(sockfd, buffer, BUFFER_SIZE - 1);
    if (received == 0) {
        throw ReadException();
    }
    for (int i = 0; i < received; ++i) {
        untreatedInput.push_back(buffer[i]);
    }
}

void Connection::sendSomething() {
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
            return;
        }

        for (int i = 0; i < send; ++i) {
            sendQueue.pop_front();
        }

    } while (send != 0 && hasSomethingToSend());
}

void Connection::addMessage(const std::string &message) {
    uint32_t length = message.size();
    uint32_t netLength = htonl(length);

    char buffer[length + sizeof(uint32_t)];

    memcpy(buffer, (char *) &netLength, sizeof(uint32_t));
    memcpy(buffer + sizeof(uint32_t), message.c_str(), length);

    for (uint32_t idx = 0; idx < length + sizeof(uint32_t); ++idx) {
        sendQueue.push_back(buffer[idx]);
    }

    while (hasSomethingToSend()) {
        pollfd polls[1];
        polls[0] = {sockfd, POLLOUT, 0};
        int pollResult = poll(polls, 1, -1);
        if (pollResult < -1) {
            return;
        }
        sendSomething();
    }
}

Connection::Connection(int sockfd) {
    this->sockfd = sockfd;
}

int Connection::getSockfd(char * hostname, char * port) {
    addrinfo hints = {};
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    addrinfo * addrs;

    int err = getaddrinfo(hostname, port, &hints, &addrs);
    if (err != 0) {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    int sockfd;

    for(struct addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next) {

        sockfd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);

        if (sockfd == -1) {
            freeaddrinfo(addrs);
            std::cerr << "Error while connection.";
            exit(1);
        }

        int connectResult = connect(sockfd, addr->ai_addr, addr->ai_addrlen);

        if (connectResult != 0) {
            close(sockfd);
            freeaddrinfo(addrs);
            std::cerr << "Error while connection.";
            exit(1);
        }
    }

    int one = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(int));

    freeaddrinfo(addrs);

    return sockfd;
}

bool Connection::canReadMessage() const {
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

bool Connection::hasSomethingToSend() const {
    return !sendQueue.empty();
}

int main(int argc, char *argv[]) {
    if (argc < 3) {
        fprintf(stderr, "usage %s hostname port\n", argv[0]);
        exit(1);
    }

    signal(SIGPIPE, SIG_IGN);

    char* hostname = argv[1];
    char* port = argv[2];

    std::cout << "Write your nickname" << std::endl;

    std::string name;
    getline(std::cin, name);

    Connection connection = Connection::buildConnection(hostname, port);
    connection.addMessage(name);

    std::thread messageGetter([&] { connection.getMessages(); });

    std::string s;
    while (getline(std::cin, s)) {
        if (s == "/shutdown") {
            break;
        }
        connection.addMessage(s);
    }

    connection.shutdown();
    messageGetter.join();

    return 0;
}
