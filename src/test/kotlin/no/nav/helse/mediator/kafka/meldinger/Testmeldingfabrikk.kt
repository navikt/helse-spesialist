package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
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

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )
}
