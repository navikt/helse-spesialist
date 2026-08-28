package no.nav.helse.spesialist.client.spleis

import io.micrometer.core.instrument.Metrics
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import no.nav.helse.spleis.rest.hentperson.Person
import java.util.UUID

/**
 * Skygge-sjekk under migreringen fra spleis sitt GraphQL-endepunkt til det nye REST-endepunktet
 * (`POST /api/person`). [graphQL] er fortsatt sannheten og returneres uendret til kalleren.
 * [hentPersonRest] (typisk [SpleisRestClient.hentPerson]) kalles seriellt etter GraphQL, kun for å
 * sammenligne det mappede resultatet mot GraphQL-resultatet.
 *
 * Designvalg (avklart med teamet):
 * - Alle feil fra REST-kallet eller -mappingen svelges og logges, aldri kastet videre.
 * - Ved avvik logges en PII-fri melding til vanlig applikasjonslogg, mens selve diff-innholdet
 *   (som kan inneholde fødselsnummer, beløp osv.) kun sendes til `tjenestekall` (sikkerlogg), i tråd
 *   med eksisterende konvensjon (se [no.nav.helse.spesialist.application.logg.loggWarn]).
 * - Sjekken styres av [skalSammenligne], som skal settes til `environmentToggles.devGcp` i første
 *   omgang — se `ClientSpleisModule`.
 */
internal class SnapshotSammenligningHenter(
    private val graphQL: Snapshothenter,
    private val hentPersonRest: (fødselsnummer: String) -> Person?,
    private val skalSammenligne: Boolean,
) : Snapshothenter {
    override fun hentPerson(fødselsnummer: String): SnapshotPerson? {
        if (!skalSammenligne) return graphQL.hentPerson(fødselsnummer)

        val graphQLResultat = graphQL.hentPerson(fødselsnummer)

        val restResultat = runCatching { hentPersonRest(fødselsnummer)?.tilSnapshotPerson() }
        sammenlign(fødselsnummer, graphQLResultat, restResultat, null)

        return graphQLResultat
    }

    private fun sammenlign(
        fødselsnummer: String,
        graphQLResultat: SnapshotPerson?,
        restResultat: Result<SnapshotPerson?>?,
        feil: Throwable?,
    ) {
        val utfall =
            when {
                feil != null || restResultat == null || restResultat.isFailure -> Utfall.FEIL
                graphQLResultat.utenFlyktigeFelter() != restResultat.getOrNull().utenFlyktigeFelter() -> Utfall.ULIKT
                graphQLResultat.utenFlyktigeFelter() == restResultat.getOrNull().utenFlyktigeFelter() -> Utfall.LIKT
                else -> Utfall.SKAL_IKKE_SKJE
            }

        sammenligningCounter(utfall).increment()

        when (utfall) {
            Utfall.FEIL ->
                loggWarn(
                    "REST-sjekk mot spleis feilet under sammenligning med GraphQL, ignorerer",
                    feil ?: restResultat?.exceptionOrNull(),
                )
            Utfall.ULIKT ->
                loggWarn(
                    "REST- og GraphQL-snapshot fra spleis er ulike",
                    "fødselsnummer" to fødselsnummer,
                    "graphQL" to graphQLResultat,
                    "rest" to restResultat?.getOrNull(),
                )
            Utfall.LIKT -> {} // ingen logging - unngå støy ved normal drift, men metrikken telles alltid
            Utfall.SKAL_IKKE_SKJE ->
                loggWarn(
                    "Uventet utfall under sammenligning av REST- og GraphQL-snapshot fra spleis",
                    "fødselsnummer" to fødselsnummer,
                    "graphQL" to graphQLResultat,
                    "rest" to restResultat?.getOrNull(),
                    "feil" to feil,
                )
        }
    }

    private enum class Utfall {
        LIKT,
        ULIKT,
        FEIL,
        SKAL_IKKE_SKJE,
    }

    private fun sammenligningCounter(utfall: Utfall) =
        Metrics.counter(
            "snapshot.sammenligning",
            "resultat",
            utfall.name.lowercase(),
        )
}

/**
 * `SnapshotBehandling.id` genereres med `UUID.randomUUID()` i spleis for hvert kall som bygger
 * snapshotet (se `SpeilGenerasjonerBuilder.mapRadTilSpeilGenerasjon`), og er derfor aldri lik mellom
 * to separate kall - selv om resten av dataene er identiske. Feltet må derfor normaliseres bort før
 * GraphQL- og REST-resultatet sammenlignes, ellers vil sammenligningen alltid rapportere avvik.
 */
private val NØYTRAL_BEHANDLINGS_ID: UUID = UUID(0, 0)

private fun SnapshotPerson?.utenFlyktigeFelter(): SnapshotPerson? =
    this?.copy(
        arbeidsgivere =
            arbeidsgivere.map { arbeidsgiver ->
                arbeidsgiver.copy(
                    behandlinger =
                        arbeidsgiver.behandlinger.map { behandling ->
                            behandling.copy(id = NØYTRAL_BEHANDLINGS_ID)
                        },
                )
            },
    )
