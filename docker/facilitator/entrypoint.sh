#!/bin/bash

# Prepares configuration and starts up facilitator
FACILITATOR_THREADS=${FACILITATOR_THREADS:-1}
FACILITATOR_PORT=${FACILITATOR_PORT:-4000}
FACILITATOR_LOG_LEVEL=${FACILITATOR_LOG_LEVEL:-info}

cat << EOF
Starting up environment variables:
FACILITATOR_THREADS=$FACILITATOR_THREADS
FACILITATOR_PORT=$FACILITATOR_PORT
FACILITATOR_LOG_LEVEL=$FACILITATOR_LOG_LEVEL
EOF


cat > facilitator.conf <<EOF
facilitatorConfig {
    listeningPortRange: {start: $FACILITATOR_PORT, end: $FACILITATOR_PORT}
    threadsNumber: $FACILITATOR_THREADS
}
EOF

cat > log-config.xml <<EOF
<configuration debug="false">

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="${FACILITATOR_LOG_LEVEL}">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
EOF

bin/wavebeans-facilitator facilitator.conf
