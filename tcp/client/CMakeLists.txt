cmake_minimum_required(VERSION 2.8)
project(client)

find_package(Threads)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
set(CMAKE_CXX_STANDARD 14)

set(SOURCE_FILES main.cpp Client.cpp Client.h ../serialization.h ../ioutils.h)
add_executable(client ${SOURCE_FILES})
target_link_libraries(client ${CMAKE_THREAD_LIBS_INIT})