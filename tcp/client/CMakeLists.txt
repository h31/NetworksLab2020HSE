cmake_minimum_required(VERSION 2.8)
project(client)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")

set(SOURCE_FILES main.c)
include_directories("../")
add_executable(client ${SOURCE_FILES})

find_library(PTHREAD_LIBRARY pthread)
target_link_libraries (client ${PTHREAD_LIBRARY})
