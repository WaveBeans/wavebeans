FROM eclipse-temurin:11-jdk

LABEL maintainer="WaveBeans"
LABEL "com.github.actions.name"="JDK 11 with Kotlin 2.2.20"
LABEL "com.github.actions.description"="Can run java app and uses Kotlin SDK, have Chromium installed for tests"

RUN apt-get update &&\
    apt-get install -y unzip chromium &&\
    ln -s /usr/bin/chromium /usr/bin/chromium-browser

ENV CHROME_BIN=/usr/bin/chromium-browser

RUN cd /usr/lib && \
    wget -q https://github.com/JetBrains/kotlin/releases/download/v2.2.20/kotlin-compiler-2.2.20.zip && \
    unzip kotlin-compiler-*.zip && \
    rm kotlin-compiler-*.zip

ENV PATH=$PATH:/usr/lib/kotlinc/bin
