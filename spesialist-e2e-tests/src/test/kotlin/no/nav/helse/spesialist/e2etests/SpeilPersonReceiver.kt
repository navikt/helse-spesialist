package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asUUID
import com.github.navikt.tbd_libs.jackson.isMissingOrNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.Vedtaksperiode
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class SpeilPersonReceiver(
    private val testContext: TestContext,
    private val saksbehandler: Saksbehandler,
    private val tilgangsgrupper: Set<Tilgangsgruppe>,
) {
    var person: JsonNode = fetchPerson(testContext.person.aktørId)
    var tilkomneInntektskilder: JsonNode = callGetTilkomneInntektskilder()

    fun hentOppdatertPerson() {
        logg.info("Henter oppdatert person...")
        person = fetchPerson(testContext.person.aktørId)
    }

    fun hentOppdaterteTilkomneInntektskilder() {
        logg.info("Henter oppdaterte tilkomne inntektskilder...")
        tilkomneInntektskilder = callGetTilkomneInntektskilder()
    }

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
                    "ident" to saksbehandler.ident,
                    "definisjonIdString" to varsel["definisjonId"].asText(),
                )
            )
        }
        hentOppdatertPerson()
    }

    fun saksbehandlerTildelerSegSaken() {
        callGraphQL(
            operationName = "Tildeling",
            variables = mapOf(
                "oppgavereferanse" to getOppgaveId(),
            )
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerFatterVedtak() {
        callGraphQL(
            operationName = "FattVedtak",
            variables = mapOf(
                "oppgavereferanse" to getOppgaveId(),
                "begrunnelse" to "Fattet vedtak",
            )
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerLeggerOppgavePåVent(
        notatTekst: String = "",
        frist: LocalDate = LocalDate.now().plusDays(1),
        arsaker: Map<String, String> = mapOf("noe" to "Opplæring")
    ) {
        callGraphQL(
            operationName = "LeggPaVent",
            variables = mapOf(
                "oppgaveId" to getOppgaveId(),
                "notatTekst" to notatTekst,
                "frist" to frist.toString(),
                "tildeling" to true,
                "arsaker" to arsaker.map { (key, arsak) -> mapOf("_key" to key, "arsak" to arsak) },
            )
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerKommentererLagtPåVent(tekst: String = "") {
        callGraphQL(
            operationName = "LeggTilKommentar",
            variables = mapOf(
                "tekst" to tekst,
                "dialogRef" to (person["arbeidsgivere"]
                    .flatMap { it["generasjoner"] }
                    .flatMap { it["perioder"] }
                    .flatMap { it["historikkinnslag"] }
                    .find { it["__typename"].asText() == "LagtPaVent" }
                    ?.get("dialogRef")?.asInt()
                    ?: error("Fant ikke historikkinnslag for \"Lagt på vent\" på personen")),
                "saksbehandlerident" to saksbehandler.ident,
            )
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerFeilregistrererFørsteKommentarPåHistorikkinnslag() {
        callGraphQL(
            operationName = "FeilregistrerKommentar",
            variables = mapOf(
                "id" to (
                        person["arbeidsgivere"]
                            .flatMap { it["generasjoner"] }
                            .flatMap { it["perioder"] }
                            .flatMap { it["historikkinnslag"] }
                            .flatMap { it["kommentarer"] }
                            .firstOrNull()
                            ?.get("id")?.asInt()
                            ?: error("Fant ikke noen kommentar på noe historikkinnslag på personen")
                        )
            )
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerLeggerTilTilkommenInntekt(
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        ekskluderteUkedager: Collection<LocalDate>,
        notatTilBeslutter: String
    ): UUID = callHttpPost(
        relativeUrl = "api/tilkomne-inntekter",
        request = mapOf(
            "fodselsnummer" to testContext.person.fødselsnummer,
            "verdier" to mapOf(
                "organisasjonsnummer" to organisasjonsnummer,
                "periode" to mapOf(
                    "fom" to fom.toString(),
                    "tom" to tom.toString(),
                ),
                "periodebelop" to periodebeløp.toString(),
                "ekskluderteUkedager" to ekskluderteUkedager.map(LocalDate::toString),
            ),
            "notatTilBeslutter" to notatTilBeslutter
        )
    )["tilkommenInntektId"].asUUID().also {
        hentOppdaterteTilkomneInntektskilder()
    }

    fun saksbehandlerEndrerTilkommenInntekt(
        tilkommenInntektId: UUID,
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        ekskluderteUkedager: Collection<LocalDate>,
        notatTilBeslutter: String
    ) {
        callHttpPost(
            relativeUrl = "api/tilkomne-inntekter/${tilkommenInntektId}/endre",
            request = mapOf(
                "endretTil" to mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "periode" to mapOf(
                        "fom" to fom.toString(),
                        "tom" to tom.toString(),
                    ),
                    "periodebelop" to periodebeløp.toString(),
                    "ekskluderteUkedager" to ekskluderteUkedager.map(LocalDate::toString),
                ),
                "notatTilBeslutter" to notatTilBeslutter
            )
        )
        hentOppdaterteTilkomneInntektskilder()
    }

    fun saksbehandlerFjernerTilkommenInntekt(
        tilkommenInntektId: UUID,
        notatTilBeslutter: String
    ) {
        callHttpPost(
            relativeUrl = "api/tilkomne-inntekter/${tilkommenInntektId}/fjern",
            request = mapOf(
                "notatTilBeslutter" to notatTilBeslutter
            )
        )
        hentOppdaterteTilkomneInntektskilder()
    }

    fun saksbehandlerGjenoppretterTilkommenInntekt(
        tilkommenInntektId: UUID,
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        periodebeløp: BigDecimal,
        ekskluderteUkedager: Collection<LocalDate>,
        notatTilBeslutter: String
    ) {
        callHttpPost(
            relativeUrl = "api/tilkomne-inntekter/${tilkommenInntektId}/gjenopprett",
            request = mapOf(
                "endretTil" to mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "periode" to mapOf(
                        "fom" to fom.toString(),
                        "tom" to tom.toString(),
                    ),
                    "periodebelop" to periodebeløp.toString(),
                    "ekskluderteUkedager" to ekskluderteUkedager.map(LocalDate::toString),
                ),
                "notatTilBeslutter" to notatTilBeslutter
            )
        )
        hentOppdaterteTilkomneInntektskilder()
    }

    fun saksbehandlerSkjønnsfastsetter830TredjeAvsnitt() {
        callGraphQL(
            operationName = "SkjonnsfastsettelseMutation",
            variables = mapOf(
                "skjonnsfastsettelse" to mapOf(
                    "aktorId" to testContext.person.aktørId,
                    "fodselsnummer" to testContext.person.fødselsnummer,
                    "skjaringstidspunkt" to testContext.vedtaksperioder.first().skjæringstidspunkt.toString(),
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
        hentOppdatertPerson()
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
        hentOppdatertPerson()
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

    fun callGetTilkomneInntektskilder(): JsonNode =
        callHttpGet("api/personer/${testContext.person.aktørId}/tilkomne-inntektskilder")

    private fun getOppgaveId(): String =
        person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["oppgave"]["id"].asText()

    private fun callGraphQL(operationName: String, variables: Map<String, Any>) =
        GraphQL.call(operationName, saksbehandler, tilgangsgrupper, variables)

    private fun callHttpGet(relativeUrl: String): JsonNode {
        val (status, bodyAsText) = runBlocking {
            httpClient.get("http://localhost:${E2ETestApplikasjon.port}/$relativeUrl") {
                accept(ContentType.Application.Json)
                bearerAuth(E2ETestApplikasjon.apiModuleIntegrationTestFixture.token(saksbehandler, tilgangsgrupper))
            }.let { it.status to it.bodyAsText() }
        }
        logg.info("Respons fra HTTP GET: $bodyAsText")
        assertTrue(status.isSuccess()) { "Fikk HTTP-feilkode ${status.value} fra HTTP GET" }
        return objectMapper.readTree(bodyAsText)
    }

    private fun callHttpPost(relativeUrl: String, request: Any): JsonNode {
        val (status, bodyAsText) = runBlocking {
            httpClient.post("http://localhost:${E2ETestApplikasjon.port}/$relativeUrl") {
                bearerAuth(E2ETestApplikasjon.apiModuleIntegrationTestFixture.token(saksbehandler, tilgangsgrupper))
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(request)
            }.let { it.status to it.bodyAsText() }
        }
        logg.info("Respons fra HTTP POST: $bodyAsText")
        assertTrue(status.isSuccess()) { "Fikk HTTP-feilkode ${status.value} fra HTTP POST" }
        return objectMapper.readTree(bodyAsText)
    }

    companion object {
        private val httpClient: HttpClient =
            HttpClient(Apache) {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter())
                }
                engine {
                    socketTimeout = 0
                    connectTimeout = 1_000
                    connectionRequestTimeout = 1_000
                }
            }
    }
}
