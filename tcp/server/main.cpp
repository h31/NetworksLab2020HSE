#include <netinet/in.h>
#include <unistd.h>
#include <string>
#include <stdexcept>
#include <iostream>
#include <memory>
#include <vector>
#include <deque>
#include <set>
#include <chrono>
#include <csignal>
#include <cstring>
#include <fcntl.h>
#include <poll.h>
#include <climits>

static const int MAX_PORT = 65535;
static const int MIN_PORT = 1024;

static const int AUTH_OP = 0;
static const int TEXT_OP = 1;
static const int SERVER_MSG_OP = 2;

class ClientInfo {
public:
    int fd;
    std::string name;
    bool authenticated;
    std::deque<uint8_t> output_buffer;
    std::deque<uint8_t> input_buffer;

    explicit ClientInfo(int fd) : fd(fd), authenticated(false) {
    }
};

static uint16_t str_to_port(std::string &port_str) {
    size_t end_pos = 0;
    int port = std::stoi(port_str, &end_pos);
    if (port < MIN_PORT || port > MAX_PORT || end_pos != port_str.size()) {
        throw std::invalid_argument("invalid port string");
    }
    return port;
}

static uint16_t read_uint16_from_iter(const std::deque<uint8_t>::iterator &begin) {
    std::vector<uint8_t> buf(begin, begin + 2);
    return ntohs(*reinterpret_cast<uint16_t *>(&buf[0]));
}

static void append_uint16_to_buffer(std::vector<uint8_t> &buf, uint16_t value) {
    uint16_t network_ordered = htons(value);
    buf.insert(buf.end(),
               reinterpret_cast<uint8_t *>(&network_ordered),
               reinterpret_cast<uint8_t *>(&network_ordered) + 2);
}

static void append_uint64_to_buffer(std::vector<uint8_t> &buf, uint64_t value) {
    uint64_t network_ordered = htobe64(value);
    buf.insert(buf.end(), reinterpret_cast<uint8_t *>(&network_ordered),
               reinterpret_cast<uint8_t *>(&network_ordered) + 8);
}

static void append_server_msg_to_buffer(std::vector<uint8_t> &buf, std::string &name, std::string &text) {
    buf.push_back(SERVER_MSG_OP);
    auto time_since_epoch = std::chrono::system_clock::now().time_since_epoch();
    uint64_t millis = std::chrono::duration_cast<std::chrono::milliseconds>(time_since_epoch).count();

    append_uint64_to_buffer(buf, millis);
    append_uint16_to_buffer(buf, name.size());
    buf.insert(buf.end(), name.begin(), name.end());
    append_uint16_to_buffer(buf, text.size());
    buf.insert(buf.end(), text.begin(), text.end());
}

void process_input_buffer(std::vector<ClientInfo> &clients, int client_num) {
    ClientInfo &current_client = clients[client_num];
    std::deque<uint8_t> &message_buffer = current_client.input_buffer;
    bool &authenticated = current_client.authenticated;

    while (true) {
        if (message_buffer.empty()) {
            return;
        }
        int op = message_buffer[0];
        if ((!authenticated && op != AUTH_OP) || (authenticated && op != TEXT_OP)) {
            std::cerr << "RECEIVED INCORRECT MESSAGE FROM CLIENT" << std::endl;
            return;
        }
        if (message_buffer.size() < 3) {
            return;
        }
        uint16_t message_len = read_uint16_from_iter(message_buffer.begin() + 1);
        if (message_buffer.size() < 3 + message_len) {
            return;
        }

        std::string message = std::string(message_buffer.begin() + 3, message_buffer.begin() + 3 + message_len);
        message_buffer.erase(message_buffer.begin(), message_buffer.begin() + 3 + message_len);

        if (op == AUTH_OP) {
            authenticated = true;
            current_client.name = message;
        } else { // op == TEXT_OP
            std::vector<uint8_t> output_msg;
            append_server_msg_to_buffer(output_msg, current_client.name, message);

            for (ClientInfo &client: clients) {
                client.output_buffer.insert(client.output_buffer.end(), output_msg.begin(), output_msg.end());
            }
        }
    }
}

bool read_from_client(std::vector<pollfd> &fds, std::vector<ClientInfo> &clients, int client_num) {
    ClientInfo &current_client = clients[client_num];
    uint8_t bytes[256];
    int n = read(current_client.fd, bytes, 256);
    bool closed = false;
    if (n > 0) {
        current_client.input_buffer.insert(current_client.input_buffer.end(), bytes, bytes + n);
        process_input_buffer(clients, client_num);
    } else if (n == 0) {
        close(current_client.fd);
        closed = true;
    } else if (n < 0) {
        std::cerr << "ERROR reading from client: " << strerror(errno) << std::endl;
        close(current_client.fd);
        closed = true;
    }
    return closed;
}

bool write_to_client(ClientInfo &client) {
    int to_send_size = std::min<int>(256, client.output_buffer.size());
    std::vector<uint8_t> to_send(client.output_buffer.begin(), client.output_buffer.begin() + to_send_size);
    int n = write(client.fd, &to_send[0], to_send_size);
    bool closed = false;
    if (n >= 0) {
        client.output_buffer.erase(client.output_buffer.begin(), client.output_buffer.begin() + n);
    } else {
        std::cerr << "ERROR writing to client: " << strerror(errno) << std::endl;
        close(client.fd);
        closed = true;
    }
    return closed;
}

void accept_client(int server_socket, std::vector<pollfd> &fds, std::vector<ClientInfo> &clients) {
    int child_fd = accept(server_socket, nullptr, nullptr);
    if (child_fd < 0) {
        std::cerr << "ERROR on accept: " << strerror(errno) << std::endl;
        return;
    }
    fcntl(child_fd, F_SETFL, O_NONBLOCK);
    pollfd child_poll_fd = {child_fd, POLLIN};
    fds.push_back(child_poll_fd);
    clients.emplace_back(child_fd);
}

void split_buffer_to_lines(std::deque<uint8_t> &bytes, std::vector<std::string> &lines) {
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

bool read_stdin(std::deque<uint8_t> &stdin_buffer) {
    uint8_t bytes[256];
    bool close = false;
    int n = read(STDIN_FILENO, bytes, 256);
    if (n >= 0) {
        stdin_buffer.insert(stdin_buffer.end(), bytes, bytes + n);
        std::vector<std::string> lines;
        split_buffer_to_lines(stdin_buffer, lines);
        for (std::string &line: lines) {
            if (line == ":q") {
                close = true;
            }
        }
    } else {
        std::cerr << "ERROR reading stdin: " << strerror(errno) << std::endl;
        close = true;
    }
    return close;
}

void set_to_blocking_mode(int fd) {
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

void close_sockets(int server_socket, std::vector<ClientInfo> &clients) {
    for (ClientInfo &client: clients) {
        shutdown(client.fd, SHUT_WR);
        set_to_blocking_mode(client.fd);
        uint8_t bytes[256];
        int n;
        while ((n = read(client.fd, bytes, 256)) > 0) {}

        if (n == -1) {
            std::cerr << "ERROR reading from socket: " << strerror(errno) << std::endl;
        }
        close(client.fd);
    }
    close(server_socket);
}

void main_server_routine(int server_socket) {
    fcntl(STDIN_FILENO, F_SETFL, O_NONBLOCK);

    int extra_fds_num = 2;
    std::vector<pollfd> fds(extra_fds_num);
    std::vector<ClientInfo> clients;
    fds[0].fd = server_socket;
    fds[0].events = POLLIN;
    fds[1].fd = STDIN_FILENO;
    fds[1].events = POLLIN;

    std::deque<uint8_t> stdin_buffer;

    while (true) {
        int ret = poll(&fds[0], fds.size(), INT_MAX);
        if (ret < 0) {
            std::cerr << "ERROR in poll: " << strerror(errno) << std::endl;
            break;
        }

        if (fds[1].revents & POLLIN) {
            if (read_stdin(stdin_buffer)) break;
        }
        if (fds[0].revents & POLLIN) {
            accept_client(fds[0].fd, fds, clients);
        }

        for (int i = extra_fds_num; i < fds.size(); i++) {
            int client_num = i - extra_fds_num;
            bool closed = false;

            if (fds[i].revents & POLLIN) {
                closed = read_from_client(fds, clients, client_num);
            }
            if (closed) {
                fds.erase(fds.begin() + i);
                clients.erase(clients.begin() + client_num);
                i--;
                continue;
            }

            if (fds[i].revents & POLLOUT) {
                closed = write_to_client(clients[client_num]);
            }
            if (closed) {
                fds.erase(fds.begin() + i);
                clients.erase(clients.begin() + client_num);
                i--;
            }
        }

        fds[0].events = POLLIN;
        fds[1].events = POLLIN;
        for (int i = extra_fds_num; i < fds.size(); i++) {
            fds[i].events = POLLIN;
            if (!clients[i - extra_fds_num].output_buffer.empty()) {
                fds[i].events |= POLLOUT;
            }
        }
    }

    close_sockets(server_socket, clients);
}

int main(int argc, char *argv[]) {
    signal(SIGPIPE, SIG_IGN); // handling error codes from read/write is better

    uint16_t port;
    sockaddr_in server_addr{}; // zero-initialized

    if (argc < 2) {
        std::cerr << "usage: " << argv[0] << " port" << std::endl;
        return 1;
    }

    try {
        std::string port_string = argv[1];
        port = str_to_port(port_string);
    } catch (const std::exception &e) {
        std::cerr << "could not parse server port. Check that provided argument is an integer in [1024, 65545] range"
                  << std::endl;
        return 1;
    }

    int server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket < 0) {
        std::cerr << "ERROR opening socket: " << strerror(errno) << std::endl;
        return 1;
    }

    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(port);
    if (bind(server_socket, (struct sockaddr *) &server_addr, sizeof(server_addr)) < 0) {
        std::cerr << "ERROR on binding: " << strerror(errno) << std::endl;
        return 1;
    }
    fcntl(server_socket, F_SETFL, O_NONBLOCK);
    listen(server_socket, 5);
    main_server_routine(server_socket);

    return 0;
}
