FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25

WORKDIR /app

ENV TZ="Europe/Oslo"

COPY spesialist-bootstrap/build/install/app/ /app/

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.helse.spesialist.bootstrap.RapidAppKt"]
