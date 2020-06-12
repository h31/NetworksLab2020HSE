#ifndef CLIENT_H
#define CLIENT_H

#include <string>
#include <cstdint>
#include <thread>
#include <mutex>

class ChatClient {
private:
    std::string user_name;
    std::string host_name;
    uint16_t port;

    int socket_fd;

    std::thread input_thread;
    std::thread output_thread;

    std::mutex stopped_mutex;
    bool stopped = false;
public:
    ChatClient(const std::string& user_name, const std::string& host_name, uint16_t port);

    void start();

    void wait();

private:
    void input_loop();
    void output_loop();

    void close_loops();
};

#endif
