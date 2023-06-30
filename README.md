# Spesialist
![Bygg og deploy](https://github.com/navikt/helse-spesialist/workflows/Bygg%20og%20deploy/badge.svg)

## Beskrivelse
Backend for saksbehandling av sykepengesøknader

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## Bygging lokalt
Vi er per nå avhengig av naisdevice for å bygge lokalt, pga graphql schema-endepunktet i filen `spesialist-selve/build.gradle.kts`

## Oppdatere GraphQL Schema for tester
Hvis du nylig har gjort endringer i GraphQL-typer, -mutations, -queries eller -subscriptions og ønsker å teste disse endringene, er det lurt å først oppdatere lokalt test schema, slik at du får syntax highlighting, code completion og et bedre liv. Da gjør du følgende:
- Kjør opp [LocalGraphQLApi.kt](spesialist-api%2Fsrc%2Ftest%2Fkotlin%2Fno%2Fnav%2Fhelse%2Fspesialist%2Fapi%2Fgraphql%2FLocalGraphQLApi.kt) sin main-metode, det starter en lokal GraphQL-server.
- Finn filen [graphql.config.yml](spesialist-api%2Fsrc%2Ftest%2Fgraphql.config.yml) og kjør `spesialist-local`, som vil oppdatere [schema.graphql](spesialist-api%2Fsrc%2Ftest%2Fschema.graphql). 
- Husk å committe endringene i [schema.graphql](spesialist-api%2Fsrc%2Ftest%2Fschema.graphql).

## Oppdatere GraphQL Schema mot spleis
- graphqlIntrospectSchema tasken skal kjøres når man bygger. Hvis schema.graphql ikke blir oppdatert, kan man prøve å slette filen og kjøre tasken på nytt.
- For å få oppdatert de genererte .kt klasse-filene må man legge til de nye feltene i hentSnapshot.graphql. 

## Kjøre lokal GraphQL Playground
- kjør main-funksjonen i LocalGraphQLApi.kt
- gå til http://localhost:4321/graphql/playground i en nettleser

## Kjøre tester raskere
- Finn filen .testcontainers.properties, ligger ofte på hjemmeområdet ditt, eksempelvis `~/.testcontainers.properties`
- Legg til denne verdien: `testcontainers.reuse.enable=true`

## Oppgradering av gradle wrapper
- Finn nyeste versjon av gradle her: https://gradle.org/releases/
- Kjør `./gradlew wrapper --gradle-version $gradleVersjon` to ganger, ref. [dokumentasjonen til Gradle](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:upgrading_wrapper).
- Oppdater gradle-versjon i build.gradle.kts-filen: `gradleVersion = "$gradleVersjon"`

## Spesialist-opprydding-dev
En selvstendig app for å slette testpersoner, kjører kun i dev.

Spleis-testdata fyrer av et kafka-event som spesialist-opprydding-dev lytter på og sletter testpersoner.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).
