#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <cerrno>
#include <cstring>
#include <stdexcept>
#include <iostream>
#include <vector>
#include <deque>
#include <chrono>
#include <iomanip>
#include <csignal>
#include <atomic>

/*
 * message from server:
 *   1 byte      8 bytes     2 bytes    NAME_LEN bytes   2 bytes    TEXT_LEN bytes
 * | OPERATION | TIMESTAMP | NAME_LEN | NAME           | TEXT_LEN | TEXT           |
 */
static const int OP_BYTES = 1;
static const int TIMESTAMP_BYTES = 8;
static const int LEN_BYTES = 2;
static const int FIXED_PREF_BYTES = OP_BYTES + TIMESTAMP_BYTES + LEN_BYTES;

static const int AUTH_OP = 0;
static const int TEXT_OP = 1;
static const int SERVER_MSG_OP = 2;

static std::atomic<bool> stopped_reading_input(false);
static std::atomic<bool> socket_closed(false);

static time_t get_time_from_millis(uint64_t millis) {
    auto duration = std::chrono::duration<uint64_t, std::milli>(millis);
    std::chrono::system_clock::time_point time_point(duration);
    return std::chrono::system_clock::to_time_t(time_point);
}

static uint16_t read_uint16_from_iter(const std::deque<uint8_t>::iterator &begin) {
    std::vector<uint8_t> buf(begin, begin + 2);
    return ntohs(*reinterpret_cast<uint16_t *>(&buf[0]));
}

static uint64_t read_uint64_from_iter(const std::deque<uint8_t>::iterator &begin) {
    std::vector<uint8_t> buf(begin, begin + 8);
    return be64toh(*reinterpret_cast<uint64_t *>(&buf[0]));
}

static void process_message_buffer(std::deque<uint8_t> &message_buffer) {
    while (true) {
        if (message_buffer.empty()) {
            return;
        }
        int op = message_buffer[0];
        if (op != SERVER_MSG_OP) {
            std::cerr << "RECEIVED INCORRECT MESSAGE FROM SERVER" << std::endl;
            return;
        }

        if (message_buffer.size() < FIXED_PREF_BYTES) {
            return;
        }
        uint16_t name_len = read_uint16_from_iter(message_buffer.begin() + OP_BYTES + TIMESTAMP_BYTES);
        if (message_buffer.size() < FIXED_PREF_BYTES + name_len + LEN_BYTES) {
            return;
        }
        uint16_t msg_len = read_uint16_from_iter(message_buffer.begin() + FIXED_PREF_BYTES + name_len);
        if (message_buffer.size() < FIXED_PREF_BYTES + name_len + LEN_BYTES + msg_len) {
            return;
        }

        auto iter = message_buffer.begin() + OP_BYTES;

        uint64_t millis_since_epoch = read_uint64_from_iter(iter);
        time_t time = get_time_from_millis(millis_since_epoch);
        iter += TIMESTAMP_BYTES;

        std::string name(iter + LEN_BYTES, iter + LEN_BYTES + name_len);
        iter += LEN_BYTES + name_len;

        std::string msg(iter + LEN_BYTES, iter + LEN_BYTES + msg_len);
        iter += LEN_BYTES + msg_len;

        message_buffer.erase(message_buffer.begin(), iter);

        std::cout << '<' << std::put_time(std::localtime(&time), "%T") << "> [" << name << "]: " << msg << std::endl;
    }
}

static void *fd_reader_loop(void *fd_ptr) {
    int socket_fd = *(int *) fd_ptr;
    std::deque<uint8_t> message_buffer;
    uint8_t net_buffer[256];
    int n;
    while ((n = read(socket_fd, &net_buffer, 256)) > 0) {
        message_buffer.insert(message_buffer.end(), net_buffer, net_buffer + n);
        process_message_buffer(message_buffer);
    }

    if (n < 0) {
        std::cerr << "error reading from socket: " << std::strerror(errno) << std::endl;
    }

    std::cout << "closing socket" << std::endl;
    socket_closed = true;
    close(socket_fd);
    if (!stopped_reading_input) {
        std::cout << "PRESS ENTER TO CLOSE" << std::endl;
    }
    return nullptr;
}

// write may transfer fewer bytes than requested!
static int write_buffer_to_socket(std::vector<uint8_t> &buffer, int socket_fd) {
    int bytes_written = 0;
    int result = 0;
    while (bytes_written != buffer.size() && result == 0) {
        int n = write(socket_fd, &buffer[0], buffer.size() - bytes_written);
        if (n > 0) {
            bytes_written += n;
        } else {
            result = n; // -1
        }
    }
    return result; // -1 or 0
}

static void append_uint16_to_buffer(std::vector<uint8_t> &buf, uint16_t value) {
    uint16_t network_ordered = htons(value);
    buf.insert(buf.end(),
               reinterpret_cast<uint8_t *>(&network_ordered),
               reinterpret_cast<uint8_t *>(&network_ordered) + 2);
}

static int write_auth_message_to_socket(std::string &name, int socket_fd) {
    std::vector<uint8_t> buffer;
    buffer.push_back(AUTH_OP);
    append_uint16_to_buffer(buffer, name.length());
    buffer.insert(buffer.end(), name.begin(), name.end());
    return write_buffer_to_socket(buffer, socket_fd);
}

static int write_text_message_to_socket(std::string &text, int socket_fd) {
    std::vector<uint8_t> buffer;
    buffer.push_back(TEXT_OP);
    append_uint16_to_buffer(buffer, text.length());
    buffer.insert(buffer.end(), text.begin(), text.end());
    return write_buffer_to_socket(buffer, socket_fd);
}

static int open_connection(addrinfo *addrs, int &sockfd) {
    int err = 0;
    for (addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next) {
        sockfd = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (sockfd == -1) {
            err = errno;
            break;
        }

        if (connect(sockfd, addr->ai_addr, addr->ai_addrlen) == 0) {
            break;
        }

        err = errno;
        close(sockfd);
        sockfd = -1;
    }
    return err;
}

int main(int argc, char *argv[]) {
    int socket_fd = -1;
    int result;
    int err;

    if (argc < 4) {
        std::cerr << "usage " << argv[0] << " hostname port username" << std::endl;
        return 1;
    }

    char *hostname = argv[1];
    char *port = argv[2];
    std::string name = std::string(argv[3]);

    if (name.length() > std::numeric_limits<uint16_t>::max()) {
        std::cerr << "name is too long";
        return 1;
    }

    addrinfo hints{}, *addrs;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    err = getaddrinfo(hostname, port, &hints, &addrs);
    if (err != 0) {
        std::cerr << hostname << ": " << gai_strerror(err) << std::endl;
        return 1;
    }

    err = open_connection(addrs, socket_fd);
    freeaddrinfo(addrs);

    if (socket_fd == -1) {
        std::cerr << hostname << ": " << std::strerror(err) << std::endl;
        return 1;
    }

    pthread_t client_handler;
    pthread_create(&client_handler, nullptr, fd_reader_loop, &socket_fd);

    result = write_auth_message_to_socket(name, socket_fd);
    if (result != 0) {
        std::cerr << "cannot write message to socket: " << strerror(errno) << std::endl;
    }

    while (result == 0) {
        std::string msg;
        std::getline(std::cin, msg);

        if (socket_closed || msg == ":q") {
            break;
        }
        result = write_text_message_to_socket(msg, socket_fd);
        if (result < 0) {
            std::cerr << "cannot write message to socket: " << strerror(errno) << std::endl;
        }
    }

    stopped_reading_input = true;
    if (!socket_closed) {
        shutdown(socket_fd, SHUT_WR);
    }

    pthread_join(client_handler, nullptr);
    return 0;
}
