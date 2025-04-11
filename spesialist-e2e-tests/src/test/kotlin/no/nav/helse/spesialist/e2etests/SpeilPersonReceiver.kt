package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.isMissingOrNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class SpeilPersonReceiver(
    private val testContext: TestContext,
    private val saksbehandlerIdent: String,
    private val bearerAuthToken: String
) {
    val person: JsonNode = fetchPerson(testContext.person.aktørId)

    fun saksbehandlerGodkjennerAlleVarsler() {
        person["arbeidsgivere"].flatMap { arbeidsgiver ->
            arbeidsgiver["generasjoner"].flatMap { generasjon ->
                generasjon["perioder"].flatMap { periode ->
                    periode["varsler"]
                }
            }
        }.forEach { varsel ->
            callGraphQL(
                operationName = "SettVarselStatus",
                variables = mapOf(
                    "generasjonIdString" to varsel["generasjonId"].asText(),
                    "varselkode" to varsel["kode"].asText(),
                    "ident" to saksbehandlerIdent,
                    "definisjonIdString" to varsel["definisjonId"].asText(),
                )
            )
        }
    }

    fun saksbehandlerTildelerSegSaken() {
        callGraphQL(
            operationName = "Tildeling",
            variables = mapOf(
                "oppgavereferanse" to getOppgaveId(),
            )
        )
    }

    fun saksbehandlerFatterVedtak() {
        callGraphQL(
            operationName = "FattVedtak",
            variables = mapOf(
                "oppgavereferanse" to getOppgaveId(),
                "begrunnelse" to "Fattet vedtak",
            )
        )
    }

    fun saksbehandlerLeggerTilTilkommenInntekt(
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        dager: Collection<LocalDate>,
        notatTilBeslutter: String
    ) {
        callGraphQL(
            operationName = "LeggTilTilkommenInntekt",
            variables = mapOf(
                "fodselsnummer" to testContext.person.fødselsnummer,
                "verdier" to mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "periode" to mapOf(
                        "fom" to fom.toString(),
                        "tom" to tom.toString(),
                    ),
                    "periodebelop" to periodebeløp.toString(),
                    "dager" to dager.map(LocalDate::toString),
                ),
                "notatTilBeslutter" to notatTilBeslutter
            )
        )
    }

    fun saksbehandlerEndrerTilkommenInntekt(
        tilkommenInntektId: UUID,
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        dager: Collection<LocalDate>,
        notatTilBeslutter: String
    ) {
        callGraphQL(
            operationName = "EndreTilkommenInntekt",
            variables = mapOf(
                "tilkommenInntektId" to tilkommenInntektId.toString(),
                "endretTil" to mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "periode" to mapOf(
                        "fom" to fom.toString(),
                        "tom" to tom.toString(),
                    ),
                    "periodebelop" to periodebeløp.toString(),
                    "dager" to dager.map(LocalDate::toString),
                ),
                "notatTilBeslutter" to notatTilBeslutter
            )
        )
    }

    fun saksbehandlerFjernerTilkommenInntekt(
        tilkommenInntektId: UUID,
        notatTilBeslutter: String
    ) {
        callGraphQL(
            operationName = "FjernTilkommenInntekt",
            variables = mapOf(
                "tilkommenInntektId" to tilkommenInntektId.toString(),
                "notatTilBeslutter" to notatTilBeslutter
            )
        )
    }

    fun saksbehandlerGjenoppretterTilkommenInntekt(
        tilkommenInntektId: UUID,
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        dager: Collection<LocalDate>,
        notatTilBeslutter: String
    ) {
        callGraphQL(
            operationName = "GjenopprettTilkommenInntekt",
            variables = mapOf(
                "tilkommenInntektId" to tilkommenInntektId.toString(),
                "endretTil" to mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "periode" to mapOf(
                        "fom" to fom.toString(),
                        "tom" to tom.toString(),
                    ),
                    "periodebelop" to periodebeløp.toString(),
                    "dager" to dager.map(LocalDate::toString),
                ),
                "notatTilBeslutter" to notatTilBeslutter
            )
        )
    }

    fun saksbehandlerSkjønnsfastsetter830TredjeAvsnitt() {
        callGraphQL(
            operationName = "SkjonnsfastsettelseMutation",
            variables = mapOf(
                "skjonnsfastsettelse" to mapOf(
                    "aktorId" to testContext.person.aktørId,
                    "fodselsnummer" to testContext.person.fødselsnummer,
                    "skjaringstidspunkt" to "2023-11-01",
                    "vedtaksperiodeId" to testContext.vedtaksperioder.first().vedtaksperiodeId,
                    "arbeidsgivere" to listOf(
                        mapOf(
                            "arlig" to 450000,
                            "arsak" to "Skjønnsfastsettelse ved mangelfull eller uriktig rapportering (§ 8-30 tredje avsnitt)",
                            "lovhjemmel" to mapOf(
                                "ledd" to "3",
                                "paragraf" to "8-30",
                                "lovverk" to "folketrygdloven",
                                "lovverksversjon" to "2019-01-01"
                            ),
                            "begrunnelseFritekst" to "skjønnsfastsetter tredje avsnitt",
                            "begrunnelseKonklusjon" to "Vi har fastsatt sykepengegrunnlaget ditt til 450 000,00 kroner.",
                            "begrunnelseMal" to "Inntekten som arbeidsgiver har rapportert til Skatteetaten er mangelfull eller uriktig. Vi har derfor skjønnsfastsatt sykepengegrunnlaget ditt. Se folketrygdloven § 8-30 tredje avsnitt.\n\nMålet med den skjønnsmessige vurderingen er å komme frem til inntekten du ville hatt om du ikke hadde blitt syk.",
                            "fraArlig" to 480000,
                            "initierendeVedtaksperiodeId" to testContext.vedtaksperioder.first().vedtaksperiodeId,
                            "organisasjonsnummer" to testContext.arbeidsgiver.organisasjonsnummer,
                            "type" to "ANNET"
                        )
                    )
                )
            )
        ).also {
            if (it["data"].isMissingOrNull()) {
                error("Forventer at mutation ikke feiler, fikk: $it")
            }
        }
    }

    fun saksbehandlerSenderTilGodkjenning() {
        callGraphQL(
            "SendTilGodkjenningV2", mapOf(
                "oppgavereferanse" to getOppgaveId(),
                "vedtakBegrunnelse" to "Sender til godkjenning"
            )
        ).also {
            if (it["data"].isMissingOrNull()) {
                error("Forventer at mutation ikke feiler, fikk: $it")
            }
        }
    }

    fun assertHarOppgaveegenskap(vararg forventedeEgenskaper: String) {
        val egenskaper =
            person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["egenskaper"].map { it["egenskap"].asText() }
        assertTrue(egenskaper.containsAll(forventedeEgenskaper.toSet())) { "Forventet å finne ${forventedeEgenskaper.toSet()} i $egenskaper" }
    }

    fun assertHarIkkeOppgaveegenskap(vararg egenskaperSomSkalMangle: String) {
        val egenskaper =
            person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["egenskaper"].map { it["egenskap"].asText() }
        assertTrue(egenskaper.none { it in egenskaperSomSkalMangle.toSet() }) { "Forventet å ikke finne ${egenskaperSomSkalMangle.toSet()} i $egenskaper" }
    }

    fun assertPeriodeHarIkkeOppgave() {
        assertTrue(person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["oppgave"].isNull) {
            "Forventet at oppgave var null for perioden, men den var: " +
                    person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["oppgave"].toPrettyString()
        }
    }

    fun assertVarselkoder(expected: List<String>, vedtaksperiode: Vedtaksperiode) {
        val vedtaksperiodeFraFetchPerson = person["arbeidsgivere"].flatMap { arbeidsgiver ->
            arbeidsgiver["generasjoner"].flatMap { generasjon ->
                generasjon["perioder"]
            }
        }.find { it["vedtaksperiodeId"].asText() == vedtaksperiode.vedtaksperiodeId.toString() }
            ?: error("Fant ikke periode med vedtaksperiodeId ${vedtaksperiode.vedtaksperiodeId} i FetchPerson-svaret")

        assertEquals(expected.sorted(), vedtaksperiodeFraFetchPerson["varsler"].map { it["kode"].asText() }.sorted())
    }

    fun assertAdressebeskyttelse(expected: String) {
        assertEquals(expected, person["personinfo"]["adressebeskyttelse"].asText())
    }

    private fun fetchPerson(aktørId: String): JsonNode {
        val fetchPersonResponse = callGraphQL(
            operationName = "FetchPerson",
            variables = mapOf(
                "aktorId" to aktørId,
            )
        )
        return fetchPersonResponse["data"]["person"].takeUnless { it.isNull }
            ?: error("Fikk ikke data.person i respons fra FetchPerson. Responsen var: ${fetchPersonResponse.toPrettyString()}")
    }

    private fun getOppgaveId(): String =
        person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["oppgave"]["id"].asText()

    private fun callGraphQL(operationName: String, variables: Map<String, Any>): JsonNode {
        val (status, bodyAsText) = runBlocking {
            httpClient.post("http://localhost:${E2ETestApplikasjon.port}/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(bearerAuthToken)
                setBody(
                    mapOf(
                        "query" to (this::class.java.getResourceAsStream("/graphql/$operationName.graphql")
                            ?.use { it.reader().readText() }
                            ?: error("Fant ikke $operationName.graphql")),
                        "operationName" to operationName,
                        "variables" to variables))
            }.let { it.status to it.bodyAsText() }
        }
        logg.info("Respons fra GraphQL: $bodyAsText")
        assertTrue(status.isSuccess()) { "Fikk HTTP-feilkode ${status.value} fra GraphQL" }
        return objectMapper.readTree(bodyAsText).also {
            assertNull(it["errors"]) { "Fikk feil i GraphQL-response" }
        }
    }

    companion object {
        private val httpClient: HttpClient =
            HttpClient(Apache) {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter())
                }
                engine {
                    socketTimeout = 5_000
                    connectTimeout = 5_000
                    connectionRequestTimeout = 5_000
                }
            }
    }
}
