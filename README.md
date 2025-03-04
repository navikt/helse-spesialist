# Spesialist

![Bygg og deploy](https://github.com/navikt/helse-spesialist/workflows/Bygg%20og%20deploy/badge.svg)

## Beskrivelse

Backend for saksbehandling av sykepengesøknader

## Moduler

Spesialist er inndelt i moduler etter lag i en onion architecture:

![Lagdeling - Onionarkitektur](lagdeling-onionarkitektur.svg)

Alle lag på samme nivå har kun avhengighet og kjennskap til lag på nivå innenfor seg selv. De har ikke kjennskap til lag
på samme nivå, eller nivåer utenfor.

![Avhengigheter](avhengighetsgraf-spesialist.png)

Modulene er:

- **e2e-tests** - Integrasjonstester som sjekker detaljer i resultat av forskjellige typer ekstern input til Spesialist.
  Simulerer konsumenter i så stor grad som mulig. Er avhengig av en ferdigsammensydd applikasjon fordi den tester den
  satt i system. Tester er black box med unntak av verifisering av forventet resultat (lagring i databaser, nye
  meldinger publisert, JSON-svar via HTTP, etc.).
- **bootstrap** - Instansiering av alle implementasjoner av hvert interface (dependency injection). Sørger for at
  applikasjonen starter med riktig oppsett, og syr sådan sammen alle lagene til noe kjørbart.
- **api-schema** - GraphQL-schema

### Infrastructure
- **kafka** - Integrasjon med Rapids and Rivers. All kunnskap om eksterne meldinger med syntaks. Serialisering /
  deserialisering av meldinger fra/til JSON
- **api** - implementasjon av det. Eventuelle andre HTTP-baserte endepunkt (f.eks. REST API).
  Eksponerer egne typer og ikke direkte domenemodeller.
- **db** - Faktisk integrasjon med og oppsett av databaser, med kunnskap om tabeller, kolonner og format. Mapper til og
  fra domeneobjekter, eller DTO-er.

### Application
- **application** - Interfaces for de abstrakte tingene infrastrukturslagene tar seg av med implementasjoner, f.eks.
  mulighet til å publisere hendelser og Repositories for å lagre/hente domeneobjekter. Kommando-oppsettet ligger også
  her. Inneholder også koordineringen ved én operasjon som medfører en serie av operasjoner på domeneobjekter.

### Domain
- **domain** - Det innerste laget som inneholder en modell av domenet applikasjonen representerer og så mange
  forretningsregler som mulig. Inneholder også tester som representerer regler i domenet.

## Komme i gang

### Gradleoppsett

#### Ktlint

Vi bruker [Ktlint](https://github.com/pinterest/ktlint) for linting av koden, for å sette dette opp må man kjøre
følgende kommando:

```shell
./gradlew addKtlintFormatGitPreCommitHook
```

Det holder å kjøre kommandoen en gang, så er commithooken satt opp for fremtidige commits.

#### Credentials

For å kunne kjøre gradle må du legge til `githubUser` og `githubPassword` i `~/.gradle/gradle.properties` filen.

```properties
githubUser=x-access-token
githubPassword=<token>
```

### Docker

For å kjøre tester trenger du docker. Du kan enten installere klienten fra [docker](https://www.docker.com/) eller bruke
colima.

#### Colima

```shell
brew install colima docker
```

For at colima skal virke må disse env variablene settes opp i `~/.zshrc`:

```shell
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
```

```shell
colima start
```

## Oppdatere GraphQL schema mot spleis

- Kjør tasken `graphqlIntrospectSchema` i spesialist-api, enten i IntelliJ eller fra kommandolinja
    - Fra kommandolinja: `./gradlew :spesialist-api:graphqlIntrospectSchema`
- Gjør eventuelt endringer i hva som skal hentes fra spleis i filen `hentSnapshot.graphql`
- Kjør tasken `graphqlGenerateClient` for å få generert klassene

> ℹ️ `graphqlGenerateClient` vil også kjøres som en del av et vanlig bygg, i motsetning til `graphqlIntrospectSchema`.

## Oppdatere GraphQL schema for tester

Hvis du nylig har gjort endringer i GraphQL-typer, -mutations, -queries eller -subscriptions og ønsker å teste disse
endringene, er det lurt å først oppdatere lokalt testskjema, slik at du får syntax highlighting, code completion og et
bedre liv. Da gjør du følgende:

- Kjør opp [LocalApp.kt](spesialist-bootstrap/src/test/kotlin/no/nav/helse/spesialist/bootstrap/LocalApp.kt) sin main-metode, det starter en
  lokal GraphQL-server.
- Finn filen [graphql.config.yml](spesialist-api/src/test/graphql.config.yml) og kjør `spesialist-local`, som vil
  oppdatere [schema_til_bruk_fra_tester.grapqhl](spesialist-api/src/test/schema_til_bruk_fra_tester.graphql).
- Eller trykk på oppdater-ikonet i `schema_til_bruk_fra_tester.grapqhl`, ser ut til å gjøre det samme.
- Commit endringene i `schema_til_bruk_fra_tester.graphql`.

## Kjøre lokal GraphQL Playground

- kjør main-funksjonen i LocalApp.kt
- gå til http://localhost:4321/graphql/playground i en nettleser
- TODO: finn ut hvordan man veksler inn tokenet som skrives ut under oppstart av LocalApp til et bearer token, og sett
  det som header i playgrounden

## Kjøre tester raskere

- Finn filen .testcontainers.properties, ligger ofte på hjemmeområdet ditt, eksempelvis `~/.testcontainers.properties`
- Legg til denne verdien: `testcontainers.reuse.enable=true`

## Oppgradering av gradle wrapper

- Finn nyeste versjon av gradle her: https://gradle.org/releases/
- Kjør `./gradlew wrapper --gradle-version $gradleVersjon` to ganger,
  ref. [dokumentasjonen til Gradle](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:upgrading_wrapper).
- Oppdater gradle-versjon i build.gradle.kts-filen: `gradleVersion = "$gradleVersjon"`

## Spesialist-opprydding-dev

En selvstendig app for å slette testpersoner, kjører kun i dev.

Spleis-testdata fyrer av et kafka-event som spesialist-opprydding-dev lytter på og sletter testpersoner.

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i
kanalen [#team-bømlo-værsågod](https://nav-it.slack.com/archives/C019637N90X).
