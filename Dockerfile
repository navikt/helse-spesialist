FROM gcr.io/distroless/java17@sha256:052076466984fd56979c15a9c3b7433262b0ad9aae55bc0c53d1da8ffdd829c3

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

WORKDIR app

COPY spesialist-selve/build/deps/*.jar /app/
COPY spesialist-selve/build/libs/*.jar /app/

CMD ["app.jar"]
