cmake_minimum_required(VERSION 3.15)
set (PROJECT_NAME "client" CXX)
project (${PROJECT_NAME})

set(Boost_USE_STATIC_LIBS        ON)
set(Boost_USE_DEBUG_LIBS         OFF)
set(Boost_USE_RELEASE_LIBS       ON)
set(Boost_USE_MULTITHREADED      ON)
set(Boost_USE_STATIC_RUNTIME    OFF)
set (BOOST_ROOT "../server/boost_1_66_0")

find_package(Boost 1.66.0 COMPONENTS thread REQUIRED)
if(Boost_FOUND)
    include_directories(${Boost_INCLUDE_DIRS})
endif()

set(SOURCE_FILES main.cpp client.cpp client.h socket_io.cpp socket_io.h)
add_executable(${PROJECT_NAME} ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME}
        ${Boost_LIBRARIES}
        ${Boost_FILESYSTEM_LIBRARY}
        ${Boost_SYSTEM_LIBRARY})