cmake_minimum_required(VERSION 2.8)
project(client)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(Threads REQUIRED)
include_directories(${THREADS_INCLUDE_DIR})

set(SOURCE_FILES
        main.cpp
        client.h client.cpp
        ../common/chat_message.h
        ../common/util.h
)

add_executable(client ${SOURCE_FILES})
target_link_libraries(client Threads::Threads)
