cmake_minimum_required(VERSION 2.8)
project(server)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -pthread")

set(SOURCE_FILES server.cpp server.h main.cpp client.cpp client.h)
add_executable(server ${SOURCE_FILES})
