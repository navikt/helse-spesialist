# Bygg noen artefakter vi trenger senere
FROM alpine:3.21 AS build

# GCP profiling agent
RUN mkdir -p /opt/cprof && \
  wget -q -O- https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz \
  | tar xzv -C /opt/cprof

# Bruk distroless herfra
FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=90 -XX:ActiveProcessorCount=2"

COPY --from=build /opt/cprof /opt/cprof
COPY spesialist-bootstrap/build/deps/*.jar /app/
COPY spesialist-bootstrap/build/libs/*.jar /app/

CMD ["app.jar"]
