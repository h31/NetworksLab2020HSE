#include <cstdio>
#include <cstdlib>
#include <netinet/in.h>
#include <unistd.h>
#include <pthread.h>
#include <string>
#include <vector>
#include <strings.h>
#include <queue>
#include <cstring>
#include <iostream>
#include <atomic>
#include <csignal>
#include <set>
#include <poll.h>
#include <sys/ioctl.h>
#include <unordered_map>
#include <bits/unordered_map.h>

const int32_t SERVER_BACKLOG = 100;
const int32_t BUFFER_SIZE = 4096;

struct message {
    std::string text;
};

struct context {
    std::atomic_bool should_exit{};
    int server_socket = 0;
};

struct client_context {
    std::queue<char> input;
    std::queue<char> output;
    pollfd client_fd;
};

void finish(context &global_context, pthread_t accept_loop_thread);
void read_from_console(context& global_context);
void* accept_loop(void *global_context_pointer);
void append_message(message& message, std::queue<char>& output_queue);
bool can_extract_message(std::queue<char>& input_queue);
message extract_message(std::queue<char>& input_queue);
std::vector<char> read_bytes(int32_t bytes_to_read, std::queue<char>& input_queue);
uint32_t read_int32(std::queue<char>& input_queue);
void write_int32(uint32_t value, std::queue<char>& output_queue);
void read_from_socket(int socket, std::queue<char> &input_queue);
void write_to_socket(int socket, std::queue<char>& output_queue);
uint32_t convert_to_uint32(std::vector<char>& char_vector);
client_context accept_client(int client_socket);
void process_incoming(std::unordered_map<int, client_context>& clients, int current_socket);

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
    global_context.server_socket = server_socket;

    pthread_t accept_loop_thread;
    pthread_create(&accept_loop_thread, nullptr, accept_loop, &global_context);

    read_from_console(global_context);

    finish(global_context, accept_loop_thread);

    return 0;
}

void finish(context &global_context, pthread_t accept_loop_thread) {
    global_context.should_exit = true;

    shutdown(global_context.server_socket, SHUT_RDWR);

    void *return_value = nullptr;
    pthread_join(accept_loop_thread, &return_value);
}

void* accept_loop(void *global_context_pointer) {
    auto *global_context = (context *)global_context_pointer;

    sockaddr_in cli_addr {};
    unsigned int clilen = sizeof(cli_addr);

    pollfd server_poll{};
    server_poll.fd = global_context->server_socket;
    server_poll.events = POLLIN;
    std::unordered_map<int, client_context> clients;

    client_context server_context;
    server_context.client_fd = server_poll;
    clients.emplace(server_poll.fd, server_context);

    while (!global_context->should_exit) {
        int clients_size = clients.size();
        pollfd tmp_polls[clients_size];
        bzero(tmp_polls, clients_size);
        int cur_ind = 0;
        for (const auto& kv : clients) {
            tmp_polls[cur_ind] = kv.second.client_fd;
            cur_ind++;
        }

        int status = poll(tmp_polls, clients_size, -1);
        if (status < 1) {
            global_context->should_exit = true;
            return nullptr;
        }

        for (int i = 0; i < clients_size; ++i) {
            try {
                if (!tmp_polls[i].revents) {
                    continue;
                }
                if (tmp_polls[i].revents & POLLIN) {
                    if (tmp_polls[i].fd == server_poll.fd) {
                        int client_socket = accept(server_poll.fd,
                                (sockaddr *)& cli_addr, &clilen);
                        if (client_socket == -1) {
                            continue;
                        }
                        client_context client_ctx = accept_client(client_socket);
                        clients.emplace(client_socket, client_ctx);
                    } else {
                        int current_socket = tmp_polls[i].fd;
                        int nread = 0;
                        ioctl(current_socket, FIONREAD, &nread);

                        if (nread == 0) {
                            close(current_socket);
                            tmp_polls[i].events = 0;
                            clients.erase(current_socket);
                        } else {
                            process_incoming(clients, current_socket);
                        }
                    }
                } else if (tmp_polls[i].revents & POLLOUT) {
                    if (tmp_polls[i].fd != server_poll.fd) {
                        int current_socket = tmp_polls[i].fd;
                        write_to_socket(current_socket, clients[current_socket].output);
                    }
                }
            } catch (const std::exception &e) {
                perror(e.what());
            }
        }
    }

    return nullptr;
}

void process_incoming(std::unordered_map<int, client_context>& clients, int current_socket) {
    read_from_socket(current_socket, clients[current_socket].input);
    while (can_extract_message(clients[current_socket].input)) {
        message incoming_message = extract_message(clients[current_socket].input);
        for (auto& client : clients) {
            append_message(incoming_message, client.second.output);
        }
    }
}

client_context accept_client(int client_socket) {
    pollfd new_poll{};
    new_poll.fd = client_socket;
    new_poll.events = POLLIN | POLLOUT;
    client_context client_ctx;
    client_ctx.client_fd = new_poll;
    return client_ctx;
}

void append_message(message& message, std::queue<char>& output_queue) {
    std::string text = message.text;
    write_int32(text.size(), output_queue);
    for (char &byte : text) {
        output_queue.push(byte);
    }
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

bool can_extract_message(std::queue<char>& input_queue) {
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

message extract_message(std::queue<char>& input_queue) {
    uint32_t message_size = read_int32(input_queue);
    std::vector<char> query_bytes = read_bytes(message_size, input_queue);
    return message {std::string(query_bytes.begin(), query_bytes.end())};
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
