#ifndef CLIENT_LOOP_H
#define CLIENT_LOOP_H

#include <memory>
#include <thread>

#include "blocking_queue.h"
#include "../common/chat_message.h"

class ClientContainer;

class ClientLoop {
private:
    int socket_fd;
    ClientContainer& container;
    BlockingQueue<std::shared_ptr<ChatMessage>> queue;
    std::thread input_thread;
    std::thread output_thread;

public:
    ClientLoop(int socket_fd, ClientContainer& container);

    void start();

    void stop();

    void send(std::shared_ptr<ChatMessage> message);

private:
    void input_loop();
    void output_loop();

    void stop_loops();
};

#endif
