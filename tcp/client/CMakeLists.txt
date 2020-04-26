cmake_minimum_required(VERSION 2.8)
project(client)

find_package (Threads)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
SET(CMAKE_BUILD_TYPE Debug)

set(SOURCE_FILES main.cpp)
add_executable(client ${SOURCE_FILES})
target_link_libraries (client ${CMAKE_THREAD_LIBS_INIT})
