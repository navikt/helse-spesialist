package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Testmeldingfabrikk(private val fødselsnummer: String, private val aktørId: String) {
    fun lagVedtaksperiodeEndret(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        forrigeTilstand: String = "FORRIGE_TILSTAND",
        gjeldendeTilstand: String = "GJELDENDE_TILSTAND"
    ) =
        nyHendelse(id, "vedtaksperiode_endret", mapOf(
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "organisasjonsnummer" to organisasjonsnummer,
            "gjeldendeTilstand" to gjeldendeTilstand,
            "forrigeTilstand" to forrigeTilstand
        ))


    fun lagVedtaksperiodeForkastet(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID(), organisasjonsnummer: String = "orgnr") =
        nyHendelse(id, "vedtaksperiode_forkastet", mapOf(
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "organisasjonsnummer" to organisasjonsnummer
        ))

    fun lagGodkjenningsbehov(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID(), organisasjonsnummer: String = "orgnr") =
        nyHendelse(id, "behov", mapOf(
            "@behov" to listOf("Godkjenning"),
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "periodeFom" to "${LocalDate.now()}",
            "periodeTom" to "${LocalDate.now()}",
            "warnings" to mapOf<String, Any>(
                "aktiviteter" to emptyList<Any>(),
                "kontekster" to emptyList<Any>()
            )
        ))

    fun lagPersoninfoløsning(id: UUID = UUID.randomUUID(), spleisbehovId: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID(), organisasjonsnummer: String = "orgnr") =
        nyHendelse(id, "behov", mapOf(
            "@final" to true,
            "@behov" to listOf("HentEnhet", "HentPersoninfo", "HentInfotrygdutbetalinger"),
            "spleisBehovId" to "$spleisbehovId",
            "vedtaksperiodeId" to "$vedtaksperiodeId",
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "orgnummer" to organisasjonsnummer,
            "HentInfotrygdutbetalinger" to mapOf(
                "historikkFom" to "2017-01-01",
                "historikkTom" to "2020-12-31"
            ),
            "@løsning" to mapOf(
                "HentInfotrygdutbetalinger" to listOf(
                    mapOf(
                        "fom" to "2018-01-01",
                        "tom" to "2018-01-31",
                        "dagsats" to "1000.0",
                        "grad" to "100",
                        "typetekst" to "ArbRef",
                        "organisasjonsnummer" to organisasjonsnummer
                    )
                ),
                "HentEnhet" to "0301",
                "HentPersoninfo" to mapOf(
                    "fornavn" to "Kari",
                    "mellomnavn" to "",
                    "etternavn" to "Nordmann",
                    "fødselsdato" to "1970-01-01",
                    "kjønn" to "Kvinne"
                )
            )
        ))

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )
}
