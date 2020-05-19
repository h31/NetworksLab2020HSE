#ifndef SERVER_CHAT_H
#define SERVER_CHAT_H


#include <boost/asio/thread_pool.hpp>
#include <boost/thread/mutex.hpp>
#include "client.h"

class chat {
private:
    std::atomic<bool> running;
    boost::asio::thread_pool chat_thread;
    boost::mutex mtx;
    std::vector<std::shared_ptr<client>> clients;

public:
    chat() : running(true), chat_thread(1) {};

    void remove_client(client* client);

    void add_client(int socket_fd);

    void send(const message& msg);

    void shutdown();

    ~chat() {
        shutdown();
    }
};


#endif //SERVER_CHAT_H
