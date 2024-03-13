package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull

internal class VarseldefinisjonMessage(packet: JsonMessage) {
    private val id: UUID = UUID.fromString(packet["gjeldende_definisjon.id"].asText())
    private val varselkode: String = packet["varselkode"].asText()
    private val tittel: String = packet["gjeldende_definisjon.tittel"].asText()
    private val forklaring: String? = packet["gjeldende_definisjon.forklaring"].takeUnless(JsonNode::isMissingOrNull)?.textValue()
    private val handling: String? = packet["gjeldende_definisjon.handling"].takeUnless(JsonNode::isMissingOrNull)?.textValue()
    private val avviklet: Boolean = packet["gjeldende_definisjon.avviklet"].asBoolean()
    private val opprettet: LocalDateTime = packet["gjeldende_definisjon.opprettet"].asLocalDateTime()

    private val varseldefinisjon get() = Varseldefinisjon(id, varselkode, tittel, forklaring, handling, avviklet, opprettet)

    internal fun sendInnTil(mediator: MeldingMediator) {
        mediator.håndter(varseldefinisjon)
    }
}