cmake_minimum_required(VERSION 2.8)
project(client)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -pthread")

set(SOURCE_FILES main.cpp client.cpp client.h)
add_executable(client ${SOURCE_FILES})
