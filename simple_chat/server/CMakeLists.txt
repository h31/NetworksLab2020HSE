cmake_minimum_required(VERSION 2.8)
project(server)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -g -Wextra -I.. -pthread")

set(SOURCE_FILES main.cpp ../readn.cpp)
add_executable(server ${SOURCE_FILES})
