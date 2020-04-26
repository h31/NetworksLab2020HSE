#ifndef MESSAGE_H
#define MESSAGE_H

#include <memory>
#include <cstring>
#include "../serialization.h"


class Message {
public:
    explicit Message(std::size_t size = 0) : m_size(size) {
        m_data = std::shared_ptr<char>(new char[PrefixSize + size], std::default_delete<char[]>());
        auto time = static_cast<uint64_t>(std::time(nullptr));
        auto timeBytes = intToArray(time);
        memcpy(m_data.get(), timeBytes.data(), timeBytes.size());
    }

    Message(const Message& other) = default;

    char* writeDst() {
        return m_data.get() + PrefixSize;
    }

    std::size_t size() const {
        return PrefixSize + m_size;
    }

    const char* data() const {
        return m_data.get();
    }

private:
    static const std::size_t PrefixSize = 8;
    std::size_t m_size = 0;
    std::shared_ptr<char> m_data;
};

#endif
