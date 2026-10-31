#!/usr/bin/env bash
# Compile and run (requires JDK 17+)
set -e
mkdir -p out
javac -d out src/*.java
java -cp out HotelOccupancyForecaster "$@"
