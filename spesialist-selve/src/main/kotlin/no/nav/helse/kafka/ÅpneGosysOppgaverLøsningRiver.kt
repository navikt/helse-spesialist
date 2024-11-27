package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.LoggerFactory

internal class ÅpneGosysOppgaverLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf("ÅpneOppgaver"))
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.requireKey("@id", "contextId", "hendelseId", "fødselsnummer")
            it.require("@løsning.ÅpneOppgaver.antall") {}
            it.requireKey("@løsning.ÅpneOppgaver.oppslagFeilet")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        sikkerLogg.info("Mottok melding ÅpneOppgaverMessage:\n{}", packet.toJson())
        val opprettet = packet["@opprettet"].asLocalDateTime()
        val contextId = packet["contextId"].asUUID()
        val hendelseId = packet["hendelseId"].asUUID()
        val fødselsnummer = packet["fødselsnummer"].asText()

        val antall = packet["@løsning.ÅpneOppgaver.antall"].takeUnless { it.isMissingOrNull() }?.asInt()
        val oppslagFeilet = packet["@løsning.ÅpneOppgaver.oppslagFeilet"].asBoolean()

        val åpneGosysOppgaver =
            ÅpneGosysOppgaverløsning(
                opprettet = opprettet,
                fødselsnummer = fødselsnummer,
                antall = antall,
                oppslagFeilet = oppslagFeilet,
            )

        meldingMediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = åpneGosysOppgaver,
            context = context,
        )
    }
}
