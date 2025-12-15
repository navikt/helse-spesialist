package no.nav.helse.spesialist.e2etests

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asUUID
import com.github.navikt.tbd_libs.jackson.isMissingOrNull
import no.nav.helse.spesialist.api.rest.ApiForkastingRequest
import no.nav.helse.spesialist.api.rest.ApiVedtakRequest
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Periode
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
        person["arbeidsgivere"]
            .flatMap { arbeidsgiver ->
                arbeidsgiver["generasjoner"].flatMap { generasjon ->
                    generasjon["perioder"].flatMap { periode ->
                        periode["varsler"]
                    }
                }
            }.forEach { varsel ->
                val varselId = varsel["id"].asText()
                callHttpPut(
                    relativeUrl = "api/varsler/$varselId/vurdering",
                    request =
                        mapOf(
                            "definisjonId" to varsel["definisjonId"].asText(),
                        ),
                )
            }
        hentOppdatertPerson()
    }

    fun saksbehandlerTildelerSegSaken() {
        callGraphQL(
            operationName = "Tildeling",
            variables =
                mapOf(
                    "oppgavereferanse" to getOppgaveId(),
                ),
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerFatterVedtak(
        behandlingId: UUID,
        begrunnelse: String? = null,
    ): JsonNode =
        callHttpPost(
            relativeUrl = "api/behandlinger/$behandlingId/vedtak",
            request = ApiVedtakRequest(begrunnelse),
        ).also {
            hentOppdatertPerson()
        }

    fun saksbehandlerKasterUtSaken(
        behandlingId: UUID,
        årsak: String,
        begrunnelser: List<String>,
        kommentar: String? = null,
    ): JsonNode =
        callHttpPost(
            relativeUrl = "api/behandlinger/$behandlingId/forkasting",
            request = ApiForkastingRequest(årsak, begrunnelser, kommentar),
        ).also {
            hentOppdatertPerson()
        }

    fun saksbehandlerLeggerOppgavePåVent(
        notatTekst: String = "",
        frist: LocalDate = LocalDate.now().plusDays(1),
        arsaker: Map<String, String> = mapOf("noe" to "Opplæring"),
    ) {
        callGraphQL(
            operationName = "LeggPaVent",
            variables =
                mapOf(
                    "oppgaveId" to getOppgaveId(),
                    "notatTekst" to notatTekst,
                    "frist" to frist.toString(),
                    "tildeling" to true,
                    "arsaker" to arsaker.map { (key, arsak) -> mapOf("_key" to key, "arsak" to arsak) },
                ),
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerKommentererLagtPåVent(tekst: String = "") {
        callGraphQL(
            operationName = "LeggTilKommentar",
            variables =
                mapOf(
                    "tekst" to tekst,
                    "dialogRef" to (
                        person["arbeidsgivere"]
                            .flatMap { it["generasjoner"] }
                            .flatMap { it["perioder"] }
                            .flatMap { it["historikkinnslag"] }
                            .find { it["__typename"].asText() == "LagtPaVent" }
                            ?.get("dialogRef")
                            ?.asInt()
                            ?: error("Fant ikke historikkinnslag for \"Lagt på vent\" på personen")
                    ),
                    "saksbehandlerident" to saksbehandler.ident.value,
                ),
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerFeilregistrererFørsteKommentarPåHistorikkinnslag() {
        callGraphQL(
            operationName = "FeilregistrerKommentar",
            variables =
                mapOf(
                    "id" to (
                        person["arbeidsgivere"]
                            .flatMap { it["generasjoner"] }
                            .flatMap { it["perioder"] }
                            .flatMap { it["historikkinnslag"] }
                            .flatMap { it["kommentarer"] }
                            .firstOrNull()
                            ?.get("id")
                            ?.asInt()
                            ?: error("Fant ikke noen kommentar på noe historikkinnslag på personen")
                    ),
                ),
        )
        hentOppdatertPerson()
    }

    fun saksbehandlerLeggerTilTilkommenInntekt(
        organisasjonsnummer: String,
        periode: Periode,
        periodebeløp: BigDecimal,
        ekskluderteUkedager: Collection<LocalDate>,
        notatTilBeslutter: String,
    ): UUID =
        callHttpPost(
            relativeUrl = "api/tilkomne-inntekter",
            request =
                mapOf(
                    "fodselsnummer" to testContext.person.fødselsnummer,
                    "verdier" to
                        mapOf(
                            "organisasjonsnummer" to organisasjonsnummer,
                            "periode" to
                                mapOf(
                                    "fom" to periode.fom.toString(),
                                    "tom" to periode.tom.toString(),
                                ),
                            "periodebelop" to periodebeløp.toString(),
                            "ekskluderteUkedager" to ekskluderteUkedager.map(LocalDate::toString),
                        ),
                    "notatTilBeslutter" to notatTilBeslutter,
                ),
        )["tilkommenInntektId"].asUUID().also {
            hentOppdaterteTilkomneInntektskilder()
        }

    fun saksbehandlerEndrerTilkommenInntekt(
        tilkommenInntektId: UUID,
        organisasjonsnummerEndring: Pair<String, String>?,
        periodeEndring: Pair<Periode, Periode>?,
        periodebeløpEndring: Pair<BigDecimal, BigDecimal>?,
        ekskluderteUkedagerEndring: Pair<Collection<LocalDate>, Collection<LocalDate>>?,
        notatTilBeslutter: String,
    ) {
        callHttpPatch(
            relativeUrl = "api/tilkomne-inntekter/$tilkommenInntektId",
            request =
                mapOf(
                    "endringer" to
                        listOfNotNull(
                            organisasjonsnummerEndring?.let { (fra, til) ->
                                "organisasjonsnummer" to
                                    mapOf(
                                        "fra" to fra,
                                        "til" to til,
                                    )
                            },
                            periodeEndring?.let { (fra, til) ->
                                "periode" to
                                    mapOf(
                                        "fra" to
                                            mapOf(
                                                "fom" to fra.fom.toString(),
                                                "tom" to fra.tom.toString(),
                                            ),
                                        "til" to
                                            mapOf(
                                                "fom" to til.fom.toString(),
                                                "tom" to til.tom.toString(),
                                            ),
                                    )
                            },
                            periodebeløpEndring?.let { (fra, til) ->
                                "periodebeløp" to
                                    mapOf(
                                        "fra" to fra.toString(),
                                        "til" to til.toString(),
                                    )
                            },
                            ekskluderteUkedagerEndring?.let { (fra, til) ->
                                "ekskluderteUkedager" to
                                    mapOf(
                                        "fra" to fra.map(LocalDate::toString),
                                        "til" to til.map(LocalDate::toString),
                                    )
                            },
                        ).toMap(),
                    "notatTilBeslutter" to notatTilBeslutter,
                ),
        )
        hentOppdaterteTilkomneInntektskilder()
    }

    fun saksbehandlerFjernerTilkommenInntekt(
        tilkommenInntektId: UUID,
        notatTilBeslutter: String,
    ) {
        callHttpPatch(
            relativeUrl = "api/tilkomne-inntekter/$tilkommenInntektId",
            request =
                mapOf(
                    "endringer" to
                        mapOf(
                            "fjernet" to
                                mapOf(
                                    "fra" to false,
                                    "til" to true,
                                ),
                        ),
                    "notatTilBeslutter" to notatTilBeslutter,
                ),
        )
        hentOppdaterteTilkomneInntektskilder()
    }

    fun saksbehandlerGjenoppretterTilkommenInntekt(
        tilkommenInntektId: UUID,
        organisasjonsnummerEndring: Pair<String, String>?,
        periodeEndring: Pair<Periode, Periode>?,
        periodebeløpEndring: Pair<BigDecimal, BigDecimal>?,
        ekskluderteUkedagerEndring: Pair<Collection<LocalDate>, Collection<LocalDate>>?,
        notatTilBeslutter: String,
    ) {
        callHttpPatch(
            relativeUrl = "api/tilkomne-inntekter/$tilkommenInntektId",
            request =
                mapOf(
                    "endringer" to
                        listOfNotNull(
                            "fjernet" to
                                mapOf(
                                    "fra" to true,
                                    "til" to false,
                                ),
                            organisasjonsnummerEndring?.let { (fra, til) ->
                                "organisasjonsnummer" to
                                    mapOf(
                                        "fra" to fra,
                                        "til" to til,
                                    )
                            },
                            periodeEndring?.let { (fra, til) ->
                                "periode" to
                                    mapOf(
                                        "fra" to
                                            mapOf(
                                                "fom" to fra.fom.toString(),
                                                "tom" to fra.tom.toString(),
                                            ),
                                        "til" to
                                            mapOf(
                                                "fom" to til.fom.toString(),
                                                "tom" to til.tom.toString(),
                                            ),
                                    )
                            },
                            periodebeløpEndring?.let { (fra, til) ->
                                "periodebeløp" to
                                    mapOf(
                                        "fra" to fra.toString(),
                                        "til" to til.toString(),
                                    )
                            },
                            ekskluderteUkedagerEndring?.let { (fra, til) ->
                                "ekskluderteUkedager" to
                                    mapOf(
                                        "fra" to fra.map(LocalDate::toString),
                                        "til" to til.map(LocalDate::toString),
                                    )
                            },
                        ).toMap(),
                    "notatTilBeslutter" to notatTilBeslutter,
                ),
        )
        hentOppdaterteTilkomneInntektskilder()
    }

    fun saksbehandlerSkjønnsfastsetter830TredjeAvsnitt(begrunnelseFritekst: String = "skjønnsfastsetter tredje avsnitt") {
        callGraphQL(
            operationName = "SkjonnsfastsettelseMutation",
            variables =
                mapOf(
                    "skjonnsfastsettelse" to
                        mapOf(
                            "aktorId" to testContext.person.aktørId,
                            "fodselsnummer" to testContext.person.fødselsnummer,
                            "skjaringstidspunkt" to
                                testContext.vedtaksperioder
                                    .first()
                                    .skjæringstidspunkt
                                    .toString(),
                            "vedtaksperiodeId" to testContext.vedtaksperioder.first().vedtaksperiodeId,
                            "arbeidsgivere" to
                                listOf(
                                    mapOf(
                                        "arlig" to 450000,
                                        "arsak" to "Skjønnsfastsettelse ved mangelfull eller uriktig rapportering (§ 8-30 tredje avsnitt)",
                                        "lovhjemmel" to
                                            mapOf(
                                                "ledd" to "3",
                                                "paragraf" to "8-30",
                                                "lovverk" to "folketrygdloven",
                                                "lovverksversjon" to "2019-01-01",
                                            ),
                                        "begrunnelseFritekst" to begrunnelseFritekst,
                                        "begrunnelseKonklusjon" to "Vi har fastsatt sykepengegrunnlaget ditt til 450 000,00 kroner.",
                                        "begrunnelseMal" to "Inntekten som arbeidsgiver har rapportert til Skatteetaten er mangelfull eller uriktig. Vi har derfor skjønnsfastsatt sykepengegrunnlaget ditt. Se folketrygdloven § 8-30 tredje avsnitt.\n\nMålet med den skjønnsmessige vurderingen er å komme frem til inntekten du ville hatt om du ikke hadde blitt syk.",
                                        "fraArlig" to 480000,
                                        "initierendeVedtaksperiodeId" to testContext.vedtaksperioder.first().vedtaksperiodeId,
                                        "organisasjonsnummer" to testContext.arbeidsgiver.organisasjonsnummer,
                                        "type" to "ANNET",
                                    ),
                                ),
                        ),
                ),
        ).also {
            if (it["data"].isMissingOrNull()) {
                error("Forventer at mutation ikke feiler, fikk: $it")
            }
        }
        hentOppdatertPerson()
    }

    fun saksbehandlerSenderTilGodkjenning(begrunnelse: String = "Sender til godkjenning") {
        callGraphQL(
            "SendTilGodkjenningV2",
            mapOf(
                "oppgavereferanse" to getOppgaveId(),
                "vedtakBegrunnelse" to begrunnelse,
            ),
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

    fun assertVarslerHarStatus(
        status: String,
        behandlingId: UUID,
    ) {
        val vedtaksperiodeFraFetchPerson =
            person["arbeidsgivere"]
                .flatMap { arbeidsgiver ->
                    arbeidsgiver["generasjoner"].flatMap { generasjon ->
                        generasjon["perioder"]
                    }
                }.find { it["behandlingId"].asText() == behandlingId.toString() }
                ?: error("Fant ikke periode med behandlingId $behandlingId i FetchPerson-svaret")

        assertTrue(vedtaksperiodeFraFetchPerson["varsler"].map { it["vurdering"]["status"].asText() }.sorted().all { it == status })
    }

    fun assertVarselkoder(
        expected: List<String>,
        vedtaksperiode: Vedtaksperiode,
    ) {
        val vedtaksperiodeFraFetchPerson =
            person["arbeidsgivere"]
                .flatMap { arbeidsgiver ->
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
        val fetchPersonResponse =
            callGraphQL(
                operationName = "FetchPerson",
                variables =
                    mapOf(
                        "aktorId" to aktørId,
                    ),
            )
        return fetchPersonResponse["data"]["person"].takeUnless { it.isNull }
            ?: error("Fikk ikke data.person i respons fra FetchPerson. Responsen var: ${fetchPersonResponse.toPrettyString()}")
    }

    fun callGetTilkomneInntektskilder(): JsonNode = callHttpGet("api/personer/${person["personPseudoId"].asText()}/tilkomne-inntektskilder")

    private fun getOppgaveId(): String = person["arbeidsgivere"][0]["generasjoner"][0]["perioder"][0]["oppgave"]["id"].asText()

    private fun callGraphQL(
        operationName: String,
        variables: Map<String, Any>,
    ) = GraphQL.call(operationName, saksbehandler, tilgangsgrupper, variables)

    private fun callHttpGet(relativeUrl: String) = REST.get(relativeUrl, saksbehandler, tilgangsgrupper)

    private fun callHttpPut(
        relativeUrl: String,
        request: Any,
    ) = REST.put(relativeUrl, saksbehandler, tilgangsgrupper, request)

    private fun callHttpPatch(
        relativeUrl: String,
        request: Any,
    ) = REST.patch(relativeUrl, saksbehandler, tilgangsgrupper, request)

    private fun callHttpPost(
        relativeUrl: String,
        request: Any,
    ) = REST.post(relativeUrl, saksbehandler, tilgangsgrupper, request)
}
