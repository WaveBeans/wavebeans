FROM openjdk:11

MAINTAINER WaveBeans

LABEL "com.github.actions.name"="JDK 11 with Kotlin 1.7.10"
LABEL "com.github.actions.description"="Can run java app and uses Kotlin SDK"

RUN cd /usr/lib && \
    wget -q https://github.com/JetBrains/kotlin/releases/download/v1.7.10/kotlin-compiler-1.7.10.zip && \
    unzip kotlin-compiler-*.zip && \
    rm kotlin-compiler-*.zip

ENV PATH $PATH:/usr/lib/kotlinc/bin
