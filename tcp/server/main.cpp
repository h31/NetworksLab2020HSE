#include <cstdio>
#include <cstdlib>
#include <netinet/in.h>
#include <unistd.h>
#include <pthread.h>
#include <string>
#include <vector>
#include <strings.h>
#include <queue>
#include <stdexcept>
#include <cstring>
#include <iostream>
#include <atomic>
#include <csignal>

const int32_t SERVER_BACKLOG = 100;
const int32_t BUFFER_SIZE = 4096;

struct message {
    std::string text;
};

struct context;

struct handler_context {
    context *global_context;
    int client_socket;
};

struct notifier_context {
    pthread_mutex_t mutex{};
    int client_socket = 0;
    std::queue<message> messages;
    pthread_cond_t has_message{};
    pthread_t thread;
    context *global_context;
};

struct context {
    pthread_mutex_t mutex{};
    std::queue<message> messages;
    std::vector<notifier_context*> notifiers;
    pthread_cond_t has_message{};
    std::atomic_bool should_exit{};
    int server_socket;
};

enum request_type {
    REGISTER_MESSAGE = 0
};

void finish(context &global_context, pthread_t notifier_thread, pthread_t accept_loop_thread);
void read_from_console(context& global_context);
void* handle_connection(void *handler_context_pointer);
void* notify_clients(void *global_context_pointer);
void* notify_client(void *notifier_context_pointer);
void* accept_loop(void *global_context_pointer);
void accept_socket(int socket, context *context);
std::vector<char> read_bytes(int32_t bytes_to_read, int client_socket, std::queue<char>& input_queue);
void write_bytes(int socket, std::queue<char>& output_queue);
uint32_t read_int32(int socket, std::queue<char>& input_queue);
void write_int32(uint32_t value, std::queue<char>& output_queue);
void read_from_socket(int socket, std::queue<char> &input_queue);
void write_to_socket(int32_t bytes_to_write, int socket, char *buffer);

int main(int argc, char *argv[]) {
    if (argc < 2) {
        fprintf(stderr, "usage %s port\n", argv[0]);
        exit(1);
    }

    int server_socket = socket(AF_INET, SOCK_STREAM, 0);

    if (server_socket < 0) {
        perror("ERROR opening socket");
        exit(1);
    }

    auto portno = (uint16_t) atoi(argv[1]);

    sockaddr_in serv_addr {};
    bzero((char *) &serv_addr, sizeof(serv_addr));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;
    serv_addr.sin_port = htons(portno);

    if (bind(server_socket, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0) {
        perror("ERROR on binding");
        exit(1);
    }

    listen(server_socket, SERVER_BACKLOG);

    signal(SIGPIPE, SIG_IGN);

    context global_context;
    pthread_mutex_init(&global_context.mutex, nullptr);
    pthread_cond_init(&global_context.has_message, nullptr);
    global_context.server_socket = server_socket;

    pthread_t notifier_thread;
    pthread_create(&notifier_thread, nullptr, notify_clients, &global_context);

    pthread_t accept_loop_thread;
    pthread_create(&accept_loop_thread, nullptr, accept_loop, &global_context);

    read_from_console(global_context);

    finish(global_context, notifier_thread, accept_loop_thread);

    return 0;
}

void finish(context &global_context, pthread_t notifier_thread, pthread_t accept_loop_thread) {
    global_context.should_exit = true;

    pthread_mutex_lock(&global_context.mutex);
    for (auto& notifier : global_context.notifiers) {
        shutdown(notifier->client_socket, SHUT_RDWR);
    }
    shutdown(global_context.server_socket, SHUT_RDWR);
    pthread_cond_broadcast(&global_context.has_message);
    pthread_mutex_unlock(&global_context.mutex);

    void *return_value = nullptr;
    pthread_join(accept_loop_thread, &return_value);
    pthread_join(notifier_thread, &return_value);

    pthread_mutex_lock(&global_context.mutex);
    for (auto& notifier : global_context.notifiers) {
        pthread_join(notifier->thread, &return_value);
        pthread_mutex_destroy(&notifier->mutex);
        delete notifier;
    }
    pthread_mutex_unlock(&global_context.mutex);

    pthread_mutex_destroy(&global_context.mutex);
    pthread_cond_destroy(&global_context.has_message);
}

void* accept_loop(void *global_context_pointer) {
    auto *global_context = (context *)global_context_pointer;

    sockaddr_in cli_addr {};
    unsigned int clilen = sizeof(cli_addr);

    while (!global_context->should_exit) {
        try {
            int client_socket = accept(global_context->server_socket, (struct sockaddr *) &cli_addr, &clilen);

            if (client_socket == -1) {
                break;
            }

            pthread_t client_thread;
            auto *local_context = new handler_context{
                    global_context,
                    client_socket
            };

            accept_socket(client_socket, global_context);
            pthread_create(&client_thread, nullptr, handle_connection, local_context);
        } catch (const std::exception &e) {
            perror(e.what());
        }
    }

    return nullptr;
}

void register_message(context *global_context, std::string& message) {
    struct message new_message =  { message };
    pthread_mutex_lock(&global_context->mutex);
    global_context->messages.push(new_message);
    pthread_mutex_unlock(&global_context->mutex);
}

void write_messages(std::vector<message>& messages, std::queue<char>& output_queue) {
    write_int32(messages.size(), output_queue);
    for (message& current : messages) {
        std::string text = current.text;
        write_int32(text.size(), output_queue);
        for (char& byte : text) {
            output_queue.push(byte);
        }
    }
}

void accept_socket(int socket, context *context) {
    pthread_mutex_lock(&context->mutex);

    auto *notifier = new notifier_context;
    pthread_mutex_init(&notifier->mutex, nullptr);
    pthread_cond_init(&notifier->has_message, nullptr);
    notifier->client_socket = socket;
    notifier->global_context = context;
    context->notifiers.emplace_back(notifier);
    pthread_create(&notifier->thread, nullptr, notify_client, notifier);

    pthread_mutex_unlock(&context->mutex);
}

void read_from_console(context& global_context) {
    while (true) {
        std::string input;
        getline(std::cin, input);

        if (input == "/exit") {
            break;
        }
    }
}

void remove_notifier(int client_socket, context *global_context) {
    int id = -1;
    for (int i = 0; i < global_context->notifiers.size(); ++i) {
        if (global_context->notifiers[i]->client_socket == client_socket) {
            id = i;
            break;
        }
    }
    if (id != -1) {
        global_context->notifiers.erase(global_context->notifiers.begin() + id);
    }
}

void* handle_connection(void *handler_context_pointer) {
    auto* local_context = (handler_context *)handler_context_pointer;
    int client_socket = local_context->client_socket;
    context *global_context = local_context->global_context;

    try {
        std::queue<char> input_queue;
        while (!global_context->should_exit) {
            uint32_t query_type = read_int32(client_socket, input_queue);

            if (query_type == request_type::REGISTER_MESSAGE) {
                uint32_t message_size = read_int32(client_socket, input_queue);
                std::vector<char> query_bytes = read_bytes(message_size, client_socket, input_queue);
                std::string query(query_bytes.begin(), query_bytes.end());
                register_message(global_context, query);
                pthread_cond_signal(&global_context->has_message);
            } else {
                break;
            }
        }
    } catch (const std::runtime_error& e) {
        perror(e.what());
        remove_notifier(client_socket, global_context);
    }
    return nullptr;
}

void* notify_client(void *notifier_context_pointer) {
    auto *notifier = (notifier_context *)notifier_context_pointer;

    try {
        std::queue<char> output_queue;
        while (!notifier->global_context->should_exit) {
            pthread_mutex_lock(&notifier->mutex);
            while (notifier->messages.empty()) {
                pthread_cond_wait(&notifier->has_message, &notifier->mutex);
                if (notifier->global_context->should_exit) {
                    pthread_mutex_unlock(&notifier->mutex);
                    return nullptr;
                }
            }

            std::vector<message> messages;
            while (!notifier->messages.empty()) {
                messages.push_back(notifier->messages.front());
                notifier->messages.pop();
            }

            write_messages(messages, output_queue);
            write_bytes(notifier->client_socket, output_queue);
            pthread_mutex_unlock(&notifier->mutex);
        }
    } catch (const std::exception& e) {
        perror(e.what());
        remove_notifier(notifier->client_socket, notifier->global_context);
    }

    return nullptr;
}

void* notify_clients(void *global_context_pointer) {
    auto *global_context = (context *)global_context_pointer;

    while (!global_context->should_exit) {
        pthread_mutex_lock(&global_context->mutex);
        try {
            while (global_context->messages.empty()) {
                pthread_cond_wait(&global_context->has_message, &global_context->mutex);
                if (global_context->should_exit) {
                    for (auto notifier : global_context->notifiers) {
                        pthread_mutex_lock(&notifier->mutex);
                        pthread_cond_broadcast(&notifier->has_message);
                        pthread_mutex_unlock(&notifier->mutex);
                    }
                    pthread_mutex_unlock(&global_context->mutex);
                    return nullptr;
                }
            }

            std::vector<message> messages;
            while (!global_context->messages.empty()) {
                messages.push_back(global_context->messages.front());
                global_context->messages.pop();
            }

            if (!messages.empty()) {
                for (notifier_context *&notifier : global_context->notifiers) {
                    pthread_mutex_lock(&notifier->mutex);
                    for (message &current : messages) {
                        notifier->messages.push(current);
                    }

                    pthread_cond_signal(&notifier->has_message);
                    pthread_mutex_unlock(&notifier->mutex);
                }
            }
        } catch (std::exception &e) {
            perror(e.what());
        }

        pthread_mutex_unlock(&global_context->mutex);
    }
    return nullptr;
}

std::vector<char> read_bytes(int32_t bytes_to_read, int client_socket, std::queue<char>& input_queue) {
    std::vector<char> result;
    int32_t total_read = 0;

    while (result.size() < bytes_to_read) {
        while (result.size() < bytes_to_read && !input_queue.empty()) {
            result.emplace_back(input_queue.front());
            input_queue.pop();
            total_read++;
        }
        if (total_read == bytes_to_read) {
            break;
        }
        read_from_socket(client_socket, input_queue);
    }
    return result;
}

void write_bytes(int socket, std::queue<char>& output_queue) {
    char buffer[BUFFER_SIZE];
    while (!output_queue.empty()) {
        bzero(buffer, BUFFER_SIZE);
        int32_t bytes_to_write = 0;
        for (int i = 0; i < BUFFER_SIZE && !output_queue.empty(); ++i) {
            buffer[i] = output_queue.front();
            output_queue.pop();
            ++bytes_to_write;
        }
        write_to_socket(bytes_to_write, socket, buffer);
    }
}

void read_from_socket(int socket, std::queue<char> &input_queue) {
    char buffer[BUFFER_SIZE];
    bzero(buffer, BUFFER_SIZE);
    int32_t bytes_read = read(socket, buffer, BUFFER_SIZE);
    if (!bytes_read) {
        throw std::runtime_error("Unable to read the query from a client.");
    }
    for (int i = 0; i < bytes_read; ++i) {
        input_queue.push(buffer[i]);
    }
}

void write_to_socket(int32_t bytes_to_write, int socket, char *buffer) {
    int32_t total_written = 0;
    while (bytes_to_write > 0) {
        int32_t bytes_written = write(socket, buffer + total_written, bytes_to_write);
        if (!bytes_written) {
            throw std::runtime_error("Unable to send a message to a client.");
        }
        total_written += bytes_written;
        bytes_to_write -= bytes_written;
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

uint32_t read_int32(int socket, std::queue<char>& input_queue) {
    std::vector<char> char_bytes = read_bytes(sizeof(uint32_t), socket, input_queue);
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
