#ifndef CHAT_MESSAGE_H
#define CHAT_MESSAGE_H

#include <string>

struct ChatMessage {
    int64_t time;
    std::string name;
    std::string text;
};

#endif
