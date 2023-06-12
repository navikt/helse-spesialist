# Spesialist
![Bygg og deploy](https://github.com/navikt/helse-spesialist/workflows/Bygg%20og%20deploy/badge.svg)

## Beskrivelse
Backend for saksbehandling av sykepengesøknader

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## Bygging lokalt
Vi er per nå avhengig av naisdevice for å bygge lokalt, pga graphql schema-endepunktet i denne filen:
```spesialist-selve/build.gradle.kts```

## Kjøre lokal GraphQL Playground
- kjør main-funksjonen i LocalGraphQLApi.kt
- gå til http://localhost:4321/graphql/playground i en nettleser

## Kjøre tester raskere
Finn filen .testcontainers.properties, ligger ofte på hjemmeområdet ditt eks: 

```~/.testcontainers.properties```

legg til denne verdien

```testcontainers.reuse.enable=true```

## Oppgradering av gradle wrapper
Finn nyeste versjon av gradle her: https://gradle.org/releases/

```./gradlew wrapper --gradle-version $gradleVersjon```

Husk å oppdater gradle versjon i build.gradle.kts filen
```gradleVersion = "$gradleVersjon"```

## Spesialist-opprydding-dev
Det er spesialist sin egen app for å slette testpersoner.

Spleis-testdata fyrer av et kafka-event som spesialist-opprydding-dev lytter på og dermed sletter testpersoner.

### For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen [#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).
