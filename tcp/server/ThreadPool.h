#ifndef THREAD_POOL_H
#define THREAD_POOL_H

#include <thread>
#include <queue>
#include <mutex>
#include <condition_variable>

class Runnable {
public:
    virtual void run() = 0;
};

class ThreadPool {
public:
    explicit ThreadPool(int threadsNumber) {
        m_workers.reserve(threadsNumber);
        for (int i = 0; i < threadsNumber; ++i) {
            m_workers[i] = std::thread(&ThreadPool::run, this);
        }
    }

    ThreadPool(const ThreadPool& other) = delete;

    ~ThreadPool() {
        stop();
    }

    void stop() {
        m_mutex.lock();
        end = true;
        ready.notify_all();
        m_mutex.unlock();
    }

    void execute(const std::shared_ptr<Runnable>& task) {
        m_mutex.lock();
        tasks.push(task);
        ready.notify_one();
        m_mutex.unlock();
    }

private:
    bool end = false;
    std::vector<std::thread> m_workers;
    std::queue<std::shared_ptr<Runnable>> tasks;
    std::mutex m_mutex;
    std::condition_variable ready;

    void run() {
        while (!end) {
            std::unique_lock<std::mutex> lock(m_mutex);
            while (!end && tasks.empty()) {
                ready.wait(lock);
            }
            if (end) {
                return;
            }
            tasks.front()->run();
            tasks.pop();
        }
    }
};

#endif
