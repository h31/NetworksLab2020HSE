cmake_minimum_required(VERSION 2.8)
project(server)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")

set(SOURCE_FILES main.cpp)
add_executable(server ${SOURCE_FILES})
TARGET_LINK_LIBRARIES(server pthread)
