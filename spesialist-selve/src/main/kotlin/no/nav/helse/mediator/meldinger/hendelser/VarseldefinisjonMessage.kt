package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.varsel.Varseldefinisjon
import java.time.LocalDateTime
import java.util.UUID

internal class VarseldefinisjonMessage(packet: JsonMessage) {
    private val id: UUID = packet["gjeldende_definisjon.id"].asUUID()
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
