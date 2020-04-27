cmake_minimum_required(VERSION 2.8)
project(client)

set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra -pthread")

set(CMAKE_CXX_STANDARD 17)

set(SOURCE_FILES main.cpp Client.cpp Client.h Message.h Message.cpp)
add_executable(client ${SOURCE_FILES})
