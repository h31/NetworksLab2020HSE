cmake_minimum_required(VERSION 2.8)
project(server)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(
        SOURCE_FILES
        main.cpp
        accept_loop.h accept_loop.cpp
        client_loop.h client_loop.cpp
        client_container.h client_container.cpp
        server.h server.cpp
        blocking_queue.h
        ../common/chat_message.h
        ../common/util.h
)

find_package(Threads REQUIRED)
include_directories(${THREADS_INCLUDE_DIR})

add_executable(server ${SOURCE_FILES})
target_link_libraries(server Threads::Threads)
