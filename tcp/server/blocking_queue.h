#ifndef BLOCKING_QUEUE_H_
#define BLOCKING_QUEUE_H_

#include <queue>

#include <mutex>
#include <condition_variable>

class queue_closed_exception : public std::exception {};

template <class T>
class BlockingQueue {
private:
    std::queue<T> queue;
    std::mutex empty_mutex;
    std::condition_variable empty_cond;
    bool closed = false;
public:
    void push(const T& element) {
        {
            std::lock_guard<std::mutex> lock(empty_mutex);
            queue.push(element);
        }
        empty_cond.notify_one();
    }

    T pop() {
        std::unique_lock<std::mutex> lock(empty_mutex);
        empty_cond.wait(lock, [this]{ return closed || !queue.empty(); });
        if (closed) {
            lock.unlock();
            throw queue_closed_exception();
        }
        T result = queue.front();
        queue.pop();
        lock.unlock();
        return result;
    }

    void close() {
        {
            std::lock_guard<std::mutex> lock(empty_mutex);
            closed = true;
        }
        empty_cond.notify_all();
    }
};

#endif