#include <cstdio>
#include <cstdlib>
#include <netdb.h>
#include <netinet/in.h>
#include <unistd.h>
#include <cerrno>
#include <cstring>
#include <stdexcept>
#include <queue>
#include <iostream>
#include <pthread.h>
#include <atomic>
#include <ctime>
#include <poll.h>
#include <asm/ioctls.h>
#include <sys/ioctl.h>

const int32_t BUFFER_SIZE = 4096;

struct message {
    std::string text;
    std::string login;
    std::tm send_time;
};

struct handler_context {
    int server_socket = 0;
    std::atomic_bool should_exit;
    pthread_mutex_t mutex{};
    std::queue<char> input;
    std::queue<char> output;
};

void* handle_incoming(void *handler_context_pointer);
void chat_loop(handler_context *context, std::string& login);
void write_int32(uint32_t value, std::queue<char>& output_queue);
std::vector<char> read_bytes(int32_t bytes_to_read, std::queue<char>& input_queue);
uint32_t read_int32(std::queue<char>& input_queue);
uint32_t convert_to_uint32(std::vector<char>& char_vector);
void write_to_socket(int socket, std::queue<char>& output_queue);
void read_from_socket(int socket, std::queue<char> &input_queue);
std::string extract_text(std::queue<char>& input_queue);
bool can_extract_text(std::queue<char>& input_queue);
void process_incoming(handler_context *context, int current_socket);

int main(int argc, char *argv[]) {
    if (argc < 4) {
        fprintf(stderr, "usage %s hostname port login\n", argv[0]);
        exit(0);
    }

    char *hostname = argv[1];
    char *port = argv[2];
    std::string login = std::string(argv[3]);

    struct addrinfo hints = {}, *addrs;
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    int err = getaddrinfo(hostname, port, &hints, &addrs); // gethostbyname is deprecated
    if (err != 0) {
        fprintf(stderr, "%s: %s\n", hostname, gai_strerror(err));
        exit(1);
    }

    int server_socket;
    for (addrinfo *addr = addrs; addr != nullptr; addr = addr->ai_next) {
        server_socket = socket(addr->ai_family, addr->ai_socktype, addr->ai_protocol);
        if (server_socket == -1) {
            err = errno;
            break;
        }
        if (connect(server_socket, addr->ai_addr, addr->ai_addrlen) == 0) {
            break;
        }
        err = errno;
        close(server_socket);
        server_socket = -1;
    }

    freeaddrinfo(addrs);

    if (server_socket == -1) {
        fprintf(stderr, "%s: %s\n", hostname, strerror(err));
        exit(1);
    }

    pthread_t handler_thread;
    handler_context context;
    context.server_socket = server_socket;
    context.should_exit = false;
    pthread_mutex_init(&context.mutex, nullptr);
    pthread_create(&handler_thread, nullptr, handle_incoming, &context);

    chat_loop(&context, login);

    context.should_exit = true;
    void *return_value = nullptr;

    shutdown(server_socket, SHUT_RDWR);
    pthread_join(handler_thread, &return_value);
    pthread_mutex_destroy(&context.mutex);

    return 0;
}

void* handle_incoming(void *handler_context_pointer) {
    auto *context = (handler_context *)handler_context_pointer;

    pollfd server_poll{};
    server_poll.fd = context->server_socket;
    server_poll.events = POLLIN | POLLOUT;

    while (!context->should_exit) {
        int status = poll(&server_poll, 1, -1);
        if (status < 1) {
            break;
        }

        try {
            if (!server_poll.revents) {
                continue;
            }
            if (server_poll.revents & POLLIN) {
                int nread = 0;
                ioctl(context->server_socket, FIONREAD, &nread);
                if (nread == 0) {
                    break;
                } else {
                    process_incoming(context, context->server_socket);
                }
            } else if (server_poll.revents & POLLOUT) {
                pthread_mutex_lock(&context->mutex);
                write_to_socket(context->server_socket, context->output);
                pthread_mutex_unlock(&context->mutex);
            }
        }
        catch (const std::exception &e) {
            perror(e.what());
        }
    }

    shutdown(context->server_socket, SHUT_RDWR);
    pthread_mutex_destroy(&context->mutex);
    exit(0);
}

void write_message(message& message, std::queue<char>& output_queue) {
    std::string text = '<' + std::to_string(message.send_time.tm_hour) + ':' +
            std::to_string(message.send_time.tm_min) + "> " +
            '[' + message.login + "] " + message.text;
    write_int32(text.size(), output_queue);
    for (char& c : text) {
        output_queue.push(c);
    }
}

void chat_loop(handler_context *context, std::string& login) {
    try {
        while (true) {
            std::string input;
            getline(std::cin, input);

            if (input == "/exit" || std::cin.fail()) {
                break;
            }

            if (!input.empty()) {
                time_t theTime = time(nullptr);
                tm *current_time = localtime(&theTime);
                message message = { input, login, *current_time };
                pthread_mutex_lock(&context->mutex);
                write_message(message, context->output);
                pthread_mutex_unlock(&context->mutex);
            }
        }
    } catch (const std::runtime_error& e) {
        perror(e.what());
    }
}

void process_incoming(handler_context *context, int current_socket) {
    read_from_socket(current_socket, context->input);
    while (can_extract_text(context->input)) {
        std::string incoming_text = extract_text(context->input);
        std::cout << incoming_text << std::endl;
    }
}

bool can_extract_text(std::queue<char>& input_queue) {
    if (input_queue.size() < sizeof(uint32_t)) {
        return false;
    }

    std::queue<char> tmp_queue(input_queue);

    std::vector<char> char_bytes;
    for (int i = 0; i < sizeof(uint32_t); ++i) {
        char_bytes.push_back(tmp_queue.front());
        tmp_queue.pop();
    }
    uint32_t message_size = convert_to_uint32(char_bytes);
    return input_queue.size() >= sizeof(uint32_t) + message_size;
}

std::string extract_text(std::queue<char>& input_queue) {
    uint32_t message_size = read_int32(input_queue);
    std::vector<char> query_bytes = read_bytes(message_size, input_queue);
    return std::string(query_bytes.begin(), query_bytes.end());
}

void read_from_socket(int socket, std::queue<char> &input_queue) {
    char buffer[BUFFER_SIZE];
    bzero(buffer, BUFFER_SIZE);
    int32_t bytes_read = read(socket, buffer, BUFFER_SIZE);
    for (int i = 0; i < bytes_read; ++i) {
        input_queue.push(buffer[i]);
    }
}

void write_to_socket(int socket, std::queue<char>& output_queue) {
    int32_t total_written = 0;
    std::queue<char> tmp_queue(output_queue);

    char buffer[BUFFER_SIZE];
    bzero(buffer, BUFFER_SIZE);
    int bytes_to_write = 0;
    for (int i = 0; i < BUFFER_SIZE && !tmp_queue.empty(); ++i) {
        buffer[i] = tmp_queue.front();
        tmp_queue.pop();
        bytes_to_write++;
    }

    int32_t bytes_written = write(socket, buffer + total_written, bytes_to_write);
    for (int i = 0; i < bytes_written; i++) {
        output_queue.pop();
    }
}

uint32_t convert_to_uint32(std::vector<char>& char_vector) {
    char bytes[char_vector.size()];
    for (int i = 0; i < char_vector.size(); ++i) {
        bytes[i] = char_vector[i];
    }
    uint32_t result;
    memcpy(&result, bytes, sizeof(uint32_t));
    return ntohl(result);
}

uint32_t read_int32(std::queue<char>& input_queue) {
    std::vector<char> char_bytes;
    for (int i = 0; i < sizeof(uint32_t); ++i) {
        char_bytes.push_back(input_queue.front());
        input_queue.pop();
    }
    return convert_to_uint32(char_bytes);
}

std::vector<char> read_bytes(int32_t bytes_to_read, std::queue<char>& input_queue) {
    std::vector<char> char_bytes;
    for (int i = 0; i < bytes_to_read; ++i) {
        char_bytes.push_back(input_queue.front());
        input_queue.pop();
    }
    return char_bytes;
}

void write_int32(uint32_t value, std::queue<char>& output_queue) {
    uint32_t new_value = htonl(value);
    char bytes[sizeof(uint32_t)];
    memcpy(bytes, &new_value, sizeof(uint32_t));
    for (char& byte : bytes) {
        output_queue.push(byte);
    }
}
