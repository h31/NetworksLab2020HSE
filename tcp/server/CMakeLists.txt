cmake_minimum_required(VERSION 2.8)
project(server)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
set(CMAKE_CXX_STANDARD 17)

set(SOURCE_FILES main.cpp Message.h Message.cpp Client.h Client.cpp Server.h Server.cpp)
add_executable(server ${SOURCE_FILES})