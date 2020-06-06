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

const int32_t BUFFER_SIZE = 4096;

enum request_type {
    REGISTER_MESSAGE = 0
};

struct message {
    std::string text;
    std::string login;
    std::tm send_time;
};

struct handler_context {
    int server_socket;
    std::atomic_bool should_exit;
};

void* handle_incoming(void *handler_context_pointer);
void chat_loop(handler_context *context, std::string& login);
std::vector<char> read_bytes(int32_t bytes_to_read, handler_context *context, std::queue<char>& input_queue);
void write_bytes(handler_context *context, std::queue<char>& output_queue);
uint32_t read_int32(handler_context *context, std::queue<char>& input_queue);
void write_int32(uint32_t value, std::queue<char>& output_queue);
void read_from_socket(handler_context *context, std::queue<char> &input_queue);
void write_to_socket(int32_t bytes_to_write, handler_context *context, char *buffer);

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
    handler_context context { server_socket };
    pthread_create(&handler_thread, nullptr, handle_incoming, &context);

    chat_loop(&context, login);

    context.should_exit = true;
    void *return_value = nullptr;

    shutdown(server_socket, SHUT_RDWR);
    pthread_join(handler_thread, &return_value);

    return 0;
}

void* handle_incoming(void *handler_context_pointer) {
    auto *context = (handler_context *)handler_context_pointer;
    std::queue<char> input_queue;

    try {
        while (!context->should_exit) {
            uint32_t message_size = read_int32(context, input_queue);
            std::vector<char> text = read_bytes(message_size, context, input_queue);
            std::string text_string;

            for (char &c : text) {
                text_string += c;
            }
            std::cout << text_string << std::endl;
        }
    } catch (const std::exception& e) {
        perror(e.what());
        shutdown(context->server_socket, SHUT_RDWR);
        exit(0);
    }
    return nullptr;
}

void write_message(message& message, std::queue<char>& output_queue) {
    std::string text = '<' + std::to_string(message.send_time.tm_hour) + ':' +
            std::to_string(message.send_time.tm_min) + " (" + message.send_time.tm_zone + ")> " +
            '[' + message.login + "] " + message.text;
    write_int32(text.size(), output_queue);
    for (char& c : text) {
        output_queue.push(c);
    }
}

void chat_loop(handler_context *context, std::string& login) {
    try {
        std::queue<char> output_queue;
        while (true) {
            std::string input;
            getline(std::cin, input);

            if (input == "/exit" || std::cin.fail()) {
                break;
            }

            if (!input.empty()) {
                write_int32(request_type::REGISTER_MESSAGE, output_queue);
                time_t theTime = time(nullptr);
                tm *current_time = localtime(&theTime);
                message message = { input, login, *current_time };
                write_message(message, output_queue);
            }
            write_bytes(context, output_queue);
        }
    } catch (const std::runtime_error& e) {
        perror(e.what());
    }
}

std::vector<char> read_bytes(int32_t bytes_to_read, handler_context *context, std::queue<char>& input_queue) {
    std::vector<char> result;
    int32_t total_read = 0;

    while (result.size() < bytes_to_read) {
        while (result.size() < bytes_to_read && !input_queue.empty()) {
            result.emplace_back(input_queue.front());
            input_queue.pop();
            total_read++;
        }
        if (total_read == bytes_to_read || context->should_exit) {
            break;
        }
        read_from_socket(context, input_queue);
    }
    return result;
}

void write_bytes(handler_context *context, std::queue<char>& output_queue) {
    char buffer[BUFFER_SIZE];
    while (!output_queue.empty()) {
        bzero(buffer, BUFFER_SIZE);
        int32_t bytes_to_write = 0;
        for (int i = 0; i < BUFFER_SIZE && !output_queue.empty(); ++i) {
            buffer[i] = output_queue.front();
            output_queue.pop();
            ++bytes_to_write;
        }
        write_to_socket(bytes_to_write, context, buffer);
    }
}

void read_from_socket(handler_context *context, std::queue<char> &input_queue) {
    char buffer[BUFFER_SIZE];
    bzero(buffer, BUFFER_SIZE);
    int32_t bytes_read = read(context->server_socket, buffer, BUFFER_SIZE);

    if (bytes_read <= 0 || context->should_exit) {
        throw std::runtime_error("Unable to read a response from the server.");
    }
    for (int i = 0; i < bytes_read; ++i) {
        input_queue.push(buffer[i]);
    }
}

void write_to_socket(int32_t bytes_to_write, handler_context *context, char *buffer) {
    int32_t total_written = 0;
    while (bytes_to_write > 0) {
        int32_t bytes_written = write(context->server_socket, buffer + total_written, bytes_to_write);

        if (bytes_written <= 0 || context->should_exit) {
            throw std::runtime_error("Unable to send a message to the server.");
        }
        total_written += bytes_written;
        bytes_to_write -= bytes_written;
    }
}

uint32_t convert_to_uint32(std::vector<char>& char_vector) {
    uint32_t result;
    memcpy(&result, char_vector.data(), sizeof(uint32_t));
    return ntohl(result);
}

uint32_t read_int32(handler_context *context, std::queue<char>& input_queue) {
    std::vector<char> char_bytes = read_bytes(sizeof(uint32_t), context, input_queue);
    return convert_to_uint32(char_bytes);
}

void write_int32(uint32_t value, std::queue<char>& output_queue) {
    uint32_t new_value = htonl(value);
    char bytes[sizeof(uint32_t)];
    memcpy(bytes, &new_value, sizeof(uint32_t));
    for (char& byte : bytes) {
        output_queue.push(byte);
    }
}
