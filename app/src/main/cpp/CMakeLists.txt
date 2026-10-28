cmake_minimum_required(VERSION 3.22.1)
project("musicengine")

# Encontrar y configurar la librería Oboe
find_package(oboe REQUIRED CONFIG)

# Crear nuestra biblioteca compartida nativa
add_library(
    musicengine
    SHARED
    audio_engine.cpp
)

# Vincular Oboe y las librerías de Android (log y android para assets)
target_link_libraries(
    musicengine
    oboe::oboe
    log
    android
)
