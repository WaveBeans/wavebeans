FROM openjdk:8

MAINTAINER WaveBeans

LABEL "com.github.actions.name"="JDK 8 with Kotlin 1.3.70"
LABEL "com.github.actions.description"="Can run java app and use Kotlin SDK"

RUN cd /usr/lib && \
    wget -q https://github.com/JetBrains/kotlin/releases/download/v1.3.70/kotlin-compiler-1.3.70.zip && \
    unzip kotlin-compiler-*.zip && \
    rm kotlin-compiler-*.zip

ENV PATH $PATH:/usr/lib/kotlinc/bin
