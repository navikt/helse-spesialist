FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=90 -XX:ActiveProcessorCount=2"

COPY spesialist-bootstrap/build/deps/*.jar /app/
COPY spesialist-bootstrap/build/libs/*.jar /app/

CMD ["app.jar"]
