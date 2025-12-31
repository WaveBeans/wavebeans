FROM eclipse-temurin:11-jdk

LABEL maintainer="WaveBeans"
LABEL "com.github.actions.name"="JDK 11 with Kotlin 2.2.20"
LABEL "com.github.actions.description"="Can run java app and uses Kotlin SDK, have Google Chrome installed for tests"

RUN apt-get update && \
    apt-get install -y wget gnupg unzip && \
    wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list && \
    apt-get update && \
    apt-get install -y google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/google-chrome

RUN cd /usr/lib && \
    wget -q https://github.com/JetBrains/kotlin/releases/download/v2.2.20/kotlin-compiler-2.2.20.zip && \
    unzip kotlin-compiler-*.zip && \
    rm kotlin-compiler-*.zip

ENV PATH=$PATH:/usr/lib/kotlinc/bin