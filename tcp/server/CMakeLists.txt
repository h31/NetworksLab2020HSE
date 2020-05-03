cmake_minimum_required(VERSION 2.8)
project(server)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")

set(SOURCE_FILES main.c)
include_directories("../")
add_executable(server ${SOURCE_FILES})

find_library(PTHREAD_LIBRARY pthread)
target_link_libraries (server ${PTHREAD_LIBRARY})
