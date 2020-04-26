cmake_minimum_required(VERSION 2.8)
project(client)

set(CMAKE_CXX_STANDARD 14)

set(SOURCE_FILES main.cpp)
add_executable(client ${SOURCE_FILES})