cmake_minimum_required(VERSION 3.22.1)
project("musicengine")

# [INTEGRACIÓN QUIRÚRGICA] FetchContent para Oboe de forma robusta e independiente de Prefab
include(FetchContent)
FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG 1.8.1
)
FetchContent_MakeAvailable(oboe)

# Crear nuestra biblioteca compartida nativa
add_library(
    musicengine
    SHARED
    audio_engine.cpp
)

# Vincular Oboe y las librerías de Android (log y android para assets)
target_link_libraries(
    musicengine
    oboe
    log
    android
)
