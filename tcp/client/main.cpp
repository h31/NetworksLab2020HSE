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
#include <fcntl.h>
#include <poll.h>
#include <climits>

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

static void append_uint16_to_buffer(std::deque<uint8_t> &buf, uint16_t value) {
    uint16_t network_ordered = htons(value);
    buf.insert(buf.end(),
               reinterpret_cast<uint8_t *>(&network_ordered),
               reinterpret_cast<uint8_t *>(&network_ordered) + 2);
}

static void append_auth_message_to_buffer(std::deque<uint8_t> &buf, std::string &name) {
    buf.push_back(AUTH_OP);
    append_uint16_to_buffer(buf, name.length());
    buf.insert(buf.end(), name.begin(), name.end());
}

static void append_text_message_to_buffer(std::deque<uint8_t> &buf, std::string &text) {
    buf.push_back(TEXT_OP);
    append_uint16_to_buffer(buf, text.length());
    buf.insert(buf.end(), text.begin(), text.end());
}

static void split_buffer_to_lines(std::deque<uint8_t> &bytes, std::vector<std::string> &lines) {
    int first_unparsed_byte = 0;
    for (int i = 0; i < bytes.size(); i++) {
        if (bytes[i] == '\n') {
            std::string line(bytes.begin() + first_unparsed_byte, bytes.begin() + i);
            lines.push_back(line);
            first_unparsed_byte = i + 1;
        }
    }
    bytes.erase(bytes.begin(), bytes.begin() + first_unparsed_byte);
}

static bool read_stdin(std::deque<uint8_t> &stdin_buffer, std::deque<uint8_t> &server_output_buffer) {
    uint8_t bytes[256];
    bool close = false;
    int n = read(STDIN_FILENO, bytes, 256);
    if (n >= 0) {
        stdin_buffer.insert(stdin_buffer.end(), bytes, bytes + n);
        std::vector<std::string> lines;
        split_buffer_to_lines(stdin_buffer, lines);
        for (std::string &line: lines) {
            if (line.length() > std::numeric_limits<uint16_t>::max()) {
                std::cerr << "message is too long" << std::endl;
            } else if (line == ":q") {
                close = true;
            } else {
                append_text_message_to_buffer(server_output_buffer, line);
            }
        }
    } else {
        std::cerr << "ERROR reading stdin: " << strerror(errno) << std::endl;
        close = true;
    }
    return close;
}

static bool read_from_server(int socket_fd, std::deque<uint8_t> &socket_input_buffer) {
    uint8_t bytes[256];
    int n = read(socket_fd, bytes, 256);
    bool closed = false;
    if (n > 0) {
        socket_input_buffer.insert(socket_input_buffer.end(), bytes, bytes + n);
        process_message_buffer(socket_input_buffer);
    } else if (n == 0) {
        close(socket_fd);
        closed = true;
    } else if (n < 0) {
        std::cerr << "ERROR reading from server: " << strerror(errno) << std::endl;
        close(socket_fd);
        closed = true;
    }
    return closed;
}

static bool write_to_server(int socket_fd, std::deque<uint8_t> &socket_output_buffer) {
    int to_send_size = std::min<int>(256, socket_output_buffer.size());
    std::vector<uint8_t> to_send(socket_output_buffer.begin(), socket_output_buffer.begin() + to_send_size);
    int n = write(socket_fd, &to_send[0], to_send_size);
    bool closed = false;
    if (n >= 0) {
        socket_output_buffer.erase(socket_output_buffer.begin(), socket_output_buffer.begin() + n);
    } else {
        std::cerr << "ERROR writing to server: " << strerror(errno) << std::endl;
        close(socket_fd);
        closed = true;
    }
    return closed;
}

static void set_to_blocking_mode(int fd) {
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags == -1) {
        std::cerr << "ERROR getting flags of fd: " << strerror(errno) << std::endl;
        return;
    }
    flags &= ~O_NONBLOCK;
    if (fcntl(fd, F_SETFL, flags) < 0) {
        std::cerr << "ERROR setting socket to non-blocking mode: " << strerror(errno) << std::endl;
        return;
    }
}

static void close_socket(int socket_fd) {
    shutdown(socket_fd, SHUT_WR);
    set_to_blocking_mode(socket_fd);
    uint8_t bytes[256];
    int n;
    while ((n = read(socket_fd, bytes, 256)) > 0) {}

    if (n == -1) {
        std::cerr << "ERROR reading from socket: " << strerror(errno) << std::endl;
    }
    close(socket_fd);
}

static void main_client_routine(int socket_fd, std::string &name) {
    fcntl(socket_fd, F_SETFL, O_NONBLOCK);
    fcntl(STDIN_FILENO, F_SETFL, O_NONBLOCK);

    std::deque<uint8_t> socket_input_buffer;
    std::deque<uint8_t> socket_output_buffer;
    std::deque<uint8_t> stdin_input_buffer;

    std::vector<pollfd> fds(2);
    fds[0].fd = socket_fd;
    fds[0].events = POLLIN | POLLOUT;
    fds[1].fd = STDIN_FILENO;
    fds[1].events = POLLIN;

    append_auth_message_to_buffer(socket_output_buffer, name);
    bool socket_already_closed = false;
    while (true) {
        int ret = poll(&fds[0], fds.size(), INT_MAX);
        if (ret < 0) {
            std::cerr << "ERROR in poll: " << strerror(errno) << std::endl;
            break;
        }

        if (fds[1].revents & POLLIN) {
            if (read_stdin(stdin_input_buffer, socket_output_buffer)) break;
        }

        if (fds[0].revents & POLLIN) {
            socket_already_closed = read_from_server(socket_fd, socket_input_buffer);
            if (socket_already_closed) break;
        }

        if (fds[0].revents & POLLOUT) {
            socket_already_closed = write_to_server(socket_fd, socket_output_buffer);
            if (socket_already_closed) break;
        }

        fds[1].events = POLLIN;
        fds[0].events = POLLIN;
        if (!socket_output_buffer.empty()) {
            fds[0].events |= POLLOUT;
        }
    }

    if (!socket_already_closed) {
        close_socket(socket_fd);
    }
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
    signal(SIGPIPE, SIG_IGN); // handling error codes from read/write is better

    int socket_fd = -1;
    int err;

    if (argc < 4) {
        std::cerr << "usage " << argv[0] << " hostname port username" << std::endl;
        return 1;
    }

    char *hostname = argv[1];
    char *port = argv[2];
    std::string name = std::string(argv[3]);

    if (name.length() > std::numeric_limits<uint16_t>::max()) {
        std::cerr << "name is too long" << std::endl;
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

    main_client_routine(socket_fd, name);

    return 0;
}
