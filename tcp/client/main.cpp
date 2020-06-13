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

class ReadException : public std::exception {};
class WriteException : public std::exception {};

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
    void sendIntroMessage(const std::string &name);
    void sendMessageToChat(const std::string &message);
    void getMessages();
    void shutdown();
    ~Connection();

private:
    int sockfd;

    std::queue<char> untreatedInput;
    Message readMessage();
    uint32_t readUInt32();
    uint16_t readUInt16();
    std::string readString();
    void readSomething();
    void sendCurrentTime() const;
    void sendStringMessage(const std::string &message) const;
    void sendMessage(char * buffer, uint32_t needSend) const;
    explicit Connection(int sockfd);
    static int getSockfd(char * hostname, char * port);
};

uint16_t getCurrentTime() {
    std::time_t t = std::time(nullptr);
    std::tm * now = std::localtime(&t);
    return now->tm_hour * 60 + now->tm_min;
}

Connection::~Connection() {
    close(sockfd);
}

Connection Connection::buildConnection(char * hostname, char * port) {
    int sockfd = getSockfd(hostname, port);
    return Connection(sockfd);
}

void Connection::sendIntroMessage(const std::string &name) {
    sendStringMessage(name);
    sendCurrentTime();
}

void Connection::sendMessageToChat(const std::string &message) {
    sendStringMessage(message);
}

void Connection::shutdown() {
    ::shutdown(sockfd, SHUT_RDWR);
    char buffer[256];
    while (read(sockfd, buffer, 255) > 0);
}

void Connection::getMessages() {
    while (true) {
        try {
            Message message = readMessage();
            char time[8];
            sprintf(time, "<%02d:%02d>", message.time / 60, message.time % 60);
            std::cout << std::string(time) << " [" << message.senderName << "] " << message.message << std::endl;
        } catch (const ReadException& e) {
            std::cout << "Disconnected from server" << std::endl;
            shutdown();
            break;
        }
    }
}

Message Connection::readMessage() {
    uint16_t time = readUInt16();
    std::string message = readString();
    std::string senderName = readString();
    return Message(senderName, message, time);
}

std::string Connection::readString() {
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

uint32_t Connection::readUInt32() {
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

uint16_t Connection::readUInt16() {
    while (untreatedInput.size() < sizeof(uint16_t)) {
        readSomething();
    }
    char buffer[sizeof(uint16_t)];
    for (char& c : buffer) {
        c = untreatedInput.front();
        untreatedInput.pop();
    }
    uint16_t result;
    memcpy(&result, buffer, sizeof(uint16_t));
    return ntohs(result);
}

void Connection::readSomething() {
    static const uint32_t BUFFER_SIZE = 256;
    char buffer[BUFFER_SIZE];
    int received = read(sockfd, buffer, BUFFER_SIZE - 1);
    if (received <= 0) {
        throw ReadException();
    }
    for (int i = 0; i < received; ++i) {
        untreatedInput.push(buffer[i]);
    }
}

void Connection::sendMessage(char * buffer, uint32_t needSend) const {
    uint32_t alreadySend = 0;
    while (alreadySend != needSend) {
        int send = write(sockfd, buffer + alreadySend, needSend - alreadySend);
        if (send <= 0) {
            throw WriteException();
        }
        alreadySend += send;
    }
}

void Connection::sendCurrentTime() const {
    uint16_t netTime = htons(getCurrentTime());
    char buffer[sizeof(uint16_t)];
    memcpy(buffer, &netTime, sizeof(uint16_t));
    sendMessage(buffer, sizeof(uint16_t));
}

void Connection::sendStringMessage(const std::string &message) const {
    uint32_t length = message.size();
    uint32_t netLength = htonl(length);

    char buffer[length + sizeof(uint32_t)];

    memcpy(buffer, (char *) &netLength, sizeof(uint32_t));
    memcpy(buffer + sizeof(uint32_t), message.c_str(), length);

    uint32_t needSend = length + sizeof(uint32_t);

    sendMessage(buffer, needSend);
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
    connection.sendIntroMessage(name);

    std::thread messageGetter([&] { connection.getMessages(); });

    std::string s;
    while (getline(std::cin, s)) {
        if (s == "/shutdown") {
            break;
        }
        try {
            connection.sendMessageToChat(s);
        } catch (const WriteException &e) {
            std::cerr << "Server disconnected" << std::endl;
            break;
        }
    }

    connection.shutdown();
    messageGetter.join();

    return 0;
}
