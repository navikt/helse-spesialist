FROM ghcr.io/navikt/baseimages/temurin:17

COPY spesialist-selve/build/deps/*.jar ./
COPY spesialist-selve/build/libs/*.jar ./

ENV JAVA_OPTS="-XX:MaxRAMPercentage=90 -XX:ActiveProcessorCount=2"
