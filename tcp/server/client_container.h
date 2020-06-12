#ifndef CLIENT_CONTAINER_H
#define CLIENT_CONTAINER_H

#include <deque>
#include <memory>
#include <mutex>

#include "../common/chat_message.h"
#include "client_loop.h"

class ClientContainer {
private:
    std::mutex mutex;
    std::deque<ClientLoop> client_loops;

public:
    void add_client(int socket_fd);

    void notify_all_clients(std::shared_ptr<ChatMessage> message);

    void stop();
};

#endif
