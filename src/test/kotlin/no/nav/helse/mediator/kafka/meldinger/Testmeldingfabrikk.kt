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

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )
}
