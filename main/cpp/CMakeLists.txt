cmake_minimum_required(VERSION 3.4.1)
project("native-lib")

add_library(
        native-lib
        SHARED
        native-lib.cpp
)

# EXACT OpenCV path from your computer:
set(OpenCV_DIR
        "C:/Users/anjan/Downloads/opencv-4.12.0-android-sdk/OpenCV-android-sdk/sdk/native/jni"
)

find_package(OpenCV REQUIRED)

include_directories(${OpenCV_INCLUDE_DIRS})

find_library(log-lib log)
find_library(android-lib android)
find_library(GLESv2-lib GLESv2)

target_link_libraries(
        native-lib
        ${OpenCV_LIBS}
        ${log-lib}
        ${android-lib}
        ${GLESv2-lib}
)
