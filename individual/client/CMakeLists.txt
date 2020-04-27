cmake_minimum_required(VERSION 3.15)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -pthread")
set (PROJECT_NAME "client" CXX)
project (${PROJECT_NAME})
find_package(Threads)

set(SOURCE_FILES main.cpp client.cpp client.h socket_io.cpp socket_io.h)
add_executable(${PROJECT_NAME} ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME})