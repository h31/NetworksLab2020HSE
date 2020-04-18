#pragma once

#include <thread>

class Client {
public:
    Client(const char* hostname, const char* port, const char* name);

    ~Client();

private:
    std::string m_name;
    int m_socketFD = 0;
    std::thread m_reader;
    std::thread m_writer;

    void read_routine();

    void write_routine();

    void stop();
};
