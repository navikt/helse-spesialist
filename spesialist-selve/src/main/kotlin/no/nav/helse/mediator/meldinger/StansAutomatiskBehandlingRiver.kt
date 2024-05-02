package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.stoppautomatiskbehandling.Kilde.ISYFO
import no.nav.helse.modell.stoppautomatiskbehandling.Status
import no.nav.helse.modell.stoppautomatiskbehandling.Årsak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class StansAutomatiskBehandlingRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "stans_automatisk_behandling")
                it.requireKey("@id")
                it.requireKey("fødselsnummer")
                it.requireKey("status")
                it.requireKey("årsaker")
                it.requireKey("opprettet")
                it.requireKey("originalMelding")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        sikkerLogg.info("Mottok melding stans_automatisk_behandling:\n{}", packet.toJson())

        val fødselsnummer = packet["fødselsnummer"].asText()
        val status = Status.valueOf(packet["status"].asText())
        val årsaker = packet["årsaker"].map { Årsak.valueOf(it.asText()) }.toSet()
        val opprettet = packet["opprettet"].asLocalDateTime()
        val originalMelding = packet["originalMelding"].asText()

        mediator.stansAutomatiskBehandling(
            fødselsnummer,
            status,
            årsaker,
            opprettet,
            originalMelding,
            ISYFO,
        )
    }
}
