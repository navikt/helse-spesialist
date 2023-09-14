FROM ghcr.io/navikt/baseimages/temurin:17

COPY spesialist-selve/build/deps/*.jar ./
COPY spesialist-selve/build/libs/*.jar ./

ENV JAVA_OPTS="-XX:MaxRAMPercentage=90"
RUN echo 'java -XX:MaxRAMPercentage=90 -XX:+PrintFlagsFinal -version | grep -Ei "maxheapsize|maxram"' > /init-scripts/0-dump-memory-config.sh
