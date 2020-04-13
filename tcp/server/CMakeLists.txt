cmake_minimum_required(VERSION 2.8)
project(server)

find_package(Threads)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
set(CMAKE_CXX_STANDARD 14)

set(SOURCE_FILES main.cpp Server.cpp Server.h Client.cpp Client.h message.h ThreadPool.h ../serialization.h ../ioutils.h)
add_executable(server ${SOURCE_FILES})
target_link_libraries(server ${CMAKE_THREAD_LIBS_INIT})
