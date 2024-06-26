# Spesialist
![Bygg og deploy](https://github.com/navikt/helse-spesialist/workflows/Bygg%20og%20deploy/badge.svg)

## Beskrivelse
Backend for saksbehandling av sykepengesøknader

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## Oppdatere GraphQL schema mot spleis
- Kjør tasken `graphqlIntrospectSchema` i spesialist-api, enten i IntelliJ eller fra kommandolinja
  - Fra kommandolinja: `./gradlew :spesialist-api:graphqlIntrospectSchema`
- Gjør eventuelt endringer i hva som skal hentes fra spleis i filen `hentSnapshot.graphql`
- Kjør tasken `graphqlGenerateClient` for å få generert klassene

> ℹ️ `graphqlGenerateClient` vil også kjøres som en del av et vanlig bygg, i motsetning til `graphqlIntrospectSchema`.

## Oppdatere GraphQL schema for tester
Hvis du nylig har gjort endringer i GraphQL-typer, -mutations, -queries eller -subscriptions og ønsker å teste disse endringene, er det lurt å først oppdatere lokalt testskjema, slik at du får syntax highlighting, code completion og et bedre liv. Da gjør du følgende:
- Kjør opp [LocalApp.kt](spesialist-selve/src/test/kotlin/no/nav/helse/LocalApp.kt) sin main-metode, det starter en lokal GraphQL-server.
- Finn filen [graphql.config.yml](spesialist-api/src/test/graphql.config.yml) og kjør `spesialist-local`, som vil oppdatere [schema_til_bruk_fra_tester.grapqhl](spesialist-api/src/test/schema_til_bruk_fra_tester.graphql). 
- Commit endringene i `schema_til_bruk_fra_tester.graphql`.

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
