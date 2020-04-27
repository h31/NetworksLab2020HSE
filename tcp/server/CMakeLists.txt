cmake_minimum_required(VERSION 3.15)
project(server)

set(CMAKE_CXX_STANDARD 14)

add_executable(server main.cpp ../utils.h)
