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

static const int MAX_PORT = 65535;
static const int MIN_PORT = 1024;

static const int AUTH_OP = 0;
static const int TEXT_OP = 1;
static const int SERVER_MSG_OP = 2;

static pthread_mutex_t thread_count_mutex;
static int client_thread_count;
static pthread_cond_t thread_count_cond;

static pthread_mutex_t send_mutex;
static std::set<int> client_sockets;

static volatile std::sig_atomic_t stop = false;

static void receive_sigint(int signum) {
    stop = true;
}

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

// write may transfer fewer bytes than requested!
static int write_buffer_to_socket(std::vector<uint8_t> &buffer, int socket_fd) {
    int bytes_written = 0;
    int result = 0;
    while (bytes_written != buffer.size() && result == 0) {
        int n = write(socket_fd, &buffer[0], buffer.size() - bytes_written);
        if (n > 0) {
            bytes_written += n;
        } else if (errno != EINTR || !stop) { // do not return on SIGINT (!stop means that it is not SIGINT)
            result = n;
        }
    }
    return result; // -1 or 0
}

void process_message_buffer(std::deque<uint8_t> &message_buffer, std::string &name, bool &authenticated) {
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
            name = message;
        } else { // op == TEXT_OP
            std::vector<uint8_t> output_msg;
            append_server_msg_to_buffer(output_msg, name, message);

            pthread_mutex_lock(&send_mutex);
            for (const int &socket: client_sockets) {
                int n = write_buffer_to_socket(output_msg, socket);
                if (n < 0) {
                    std::cerr << "error writing to socket: " << strerror(errno) << std::endl;
                }
            }
            pthread_mutex_unlock(&send_mutex);
        }
    }
}

void *client_receive_loop(void *client_socket_fd_ptr) {
    pthread_detach(pthread_self());

    int client_socket = *((int *) client_socket_fd_ptr);
    delete ((int *) client_socket_fd_ptr);

    pthread_mutex_lock(&send_mutex);
    client_sockets.insert(client_socket);
    pthread_mutex_unlock(&send_mutex);

    std::string name;
    bool authenticated = false;

    std::deque<uint8_t> message_buffer;
    uint8_t net_buffer[256];
    bool continue_reading = true;
    int n = 0;
    while (continue_reading) {
        n = read(client_socket, &net_buffer, 256);
        if (n > 0) {
            message_buffer.insert(message_buffer.end(), net_buffer, net_buffer + n);
            process_message_buffer(message_buffer, name, authenticated);
        } else if (errno != EINTR || !stop) { // do not stop on SIGINT (!stop means that it is not SIGINT) This thread will
            continue_reading = false;         // terminate gracefully after reading EOF from client.
        }
    }

    if (n < 0) {
        std::cerr << "error reading from socket: " << std::strerror(errno) << std::endl;
    }
    pthread_mutex_lock(&send_mutex);
    close(client_socket);
    client_sockets.erase(client_socket);
    pthread_mutex_unlock(&send_mutex);

    pthread_mutex_lock(&thread_count_mutex);
    client_thread_count--;
    if (client_thread_count == 0) {
        pthread_cond_broadcast(&thread_count_cond);
    }
    pthread_mutex_unlock(&thread_count_mutex);
    return nullptr;
}

static void init_mutexes() {
    pthread_mutex_init(&send_mutex, nullptr);
    pthread_mutex_init(&thread_count_mutex, nullptr);
    pthread_cond_init(&thread_count_cond, nullptr);
}

static void destroy_mutexes() {
    pthread_cond_destroy(&thread_count_cond);
    pthread_mutex_destroy(&thread_count_mutex);
    pthread_mutex_destroy(&send_mutex);
}

int main(int argc, char *argv[]) {
    signal(SIGPIPE, SIG_IGN); // handling error codes from read/write is better

    struct sigaction sigint_handler{};
    sigint_handler.sa_handler = receive_sigint;
    sigemptyset(&sigint_handler.sa_mask);
    sigaction(SIGINT, &sigint_handler, nullptr);

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

    listen(server_socket, 5);
    init_mutexes();
    while (true) {
        if (stop) {
            break;
        }
        // SIGINT may be received AFTER if(stop) but BEFORE accept. In this case server will not stop immediately but
        // it is VERY unlikely and user can send SIGINT another time anyway so I believe that it is not a problem.
        int child_fd = accept(server_socket, nullptr, nullptr);
        if (stop) {
            break;
        }
        if (child_fd < 0) {
            std::cerr << "ERROR on accept: " << strerror(errno) << std::endl;
        }

        pthread_mutex_lock(&thread_count_mutex);
        client_thread_count++;
        pthread_mutex_unlock(&thread_count_mutex);

        pthread_t client_handler; // identifiers are safe to reuse
        int res = pthread_create(&client_handler, nullptr, client_receive_loop, new int(child_fd));
        if (res != 0) {
            std::cerr << "cannot create thread: " << strerror(errno) << std::endl;
            // thread was not created:
            pthread_mutex_lock(&thread_count_mutex);
            client_thread_count--;
            pthread_mutex_unlock(&thread_count_mutex);
        }
    }

    pthread_mutex_lock(&send_mutex);
    for (int client_socket: client_sockets) {
        shutdown(client_socket, SHUT_WR);
    }
    client_sockets.clear();
    pthread_mutex_unlock(&send_mutex);

    // wait for all threads to stop
    // no need to join threads because they are detached
    pthread_mutex_lock(&thread_count_mutex);
    while (client_thread_count != 0) {
        pthread_cond_wait(&thread_count_cond, &thread_count_mutex);
    }
    pthread_mutex_unlock(&thread_count_mutex);

    close(server_socket);
    destroy_mutexes();
    return 1;
}
