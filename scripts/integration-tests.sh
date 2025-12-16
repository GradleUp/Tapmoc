#!/usr/bin/env sh
set -e
set -x
cd tests/agp8-java && ./gradlew build
cd ../agp8-kotlin && ./gradlew build
cd ../agp9-kmp && ./gradlew build
cd ../agp9-kotlin && ./gradlew build
cd ../java && ./gradlew build
cd ../jvm && ./gradlew build
cd ../wasm-js && ./gradlew build
cd ../gradle-plugin && ./gradlew build
cd ../kmp && ./gradlew build
