package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.NyeVarsler.Companion.varsler
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class NyeVarslerRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandAny("@event_name", listOf("aktivitetslogg_ny_aktivitet", "nye_varsler"))
            it.requireKey("@id", "fødselsnummer")
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.requireArray("aktiviteter") {
                requireKey("melding", "nivå", "id")
                interestedIn("varselkode")
                require("tidsstempel", JsonNode::asLocalDateTime)
                requireArray("kontekster") {
                    requireKey("konteksttype", "kontekstmap")
                }
            }
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val varsler = packet["aktiviteter"].varsler()
        if (varsler.isEmpty()) return
        mediator.mottaMelding(NyeVarsler(packet), context)
    }
}
