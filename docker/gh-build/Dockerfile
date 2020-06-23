FROM openjdk:8

MAINTAINER WaveBeans

LABEL "com.github.actions.name"="JDK 8 with Kotlin 1.4-M2"
LABEL "com.github.actions.description"="Can run java app and uses Kotlin SDK"

RUN cd /usr/lib && \
    wget -q https://github.com/JetBrains/kotlin/releases/download/v1.4-M2/kotlin-compiler-1.4-M2.zip && \
    unzip kotlin-compiler-*.zip && \
    rm kotlin-compiler-*.zip

ENV PATH $PATH:/usr/lib/kotlinc/bin
