FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25

WORKDIR /app

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=90 -XX:ActiveProcessorCount=2"

COPY spesialist-bootstrap/build/install/app/ /app/

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.helse.spesialist.bootstrap.RapidAppKt"]
