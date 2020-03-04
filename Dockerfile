FROM gradle:6.1.1-jdk8

MAINTAINER WaveBeans

LABEL "com.github.actions.name"="Gradle with Kotlin 1.3.61"
LABEL "com.github.actions.description"="Runs gradle and kotlinc"

RUN cd /usr/lib && \
    wget -q https://github.com/JetBrains/kotlin/releases/download/v1.3.61/kotlin-compiler-1.3.61.zip && \
    unzip kotlin-compiler-*.zip && \
    rm kotlin-compiler-*.zip

ENV PATH $PATH:/usr/lib/kotlinc/bin
