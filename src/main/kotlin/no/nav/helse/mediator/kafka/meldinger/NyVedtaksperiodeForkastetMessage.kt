package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.*

internal class NyVedtaksperiodeForkastetMessage(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String
) : Hendelse {
    override fun håndter(mediator: ICommandMediator, context: CommandContext) {
        TODO("Not yet implemented")
    }

    override fun fødselsnummer() = fødselsnummer

    override fun vedtaksperiodeId() = vedtaksperiodeId

    override fun toJson(): String {
        return """{
    "id": "$id",
    "vedtaksperiodeId": "$vedtaksperiodeId",
    "fødselsnummer": "$fødselsnummer"
}"""
    }

    internal class VedtaksperiodeForkastetRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : River.PacketListener {

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_forkastet")
                    it.require("@id", ::uuid)
                    it.require("vedtaksperiodeId", ::uuid)
                    it.requireKey("fødselsnummer")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val vedtaksperiodeId = uuid(packet["vedtaksperiodeId"])
            val fødselsnummer = packet["fødselsnummer"].asText()
            mediator.håndter(packet, NyVedtaksperiodeForkastetMessage(uuid(packet["@id"]), vedtaksperiodeId, fødselsnummer))
        }
    }
}

private fun uuid(jsonNode: JsonNode): UUID = UUID.fromString(jsonNode.asText())
