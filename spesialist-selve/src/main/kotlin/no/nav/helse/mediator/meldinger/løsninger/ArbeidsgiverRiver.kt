package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.UUID

internal class ArbeidsgiverRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val behov = "Arbeidsgiverinformasjon"

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf(behov))
            it.requireKey("contextId", "hendelseId", "@id")
            it.requireKey("@løsning.$behov")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLog.error("forstod ikke $behov:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = UUID.fromString(packet["hendelseId"].asText())
        val contextId = UUID.fromString(packet["contextId"].asText())
        val løsning = packet["@løsning.$behov"]
        mediator.løsning(
            hendelseId,
            contextId,
            UUID.fromString(packet["@id"].asText()),
            Arbeidsgiverinformasjonløsning(
                løsning.map { arbeidsgiver ->
                    Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(
                        orgnummer = arbeidsgiver.path("orgnummer").asText(),
                        navn = arbeidsgiver.path("navn").asText(),
                        bransjer = arbeidsgiver.path("bransjer").map { it.asText() },
                    )
                },
            ),
            context,
        )
    }
}
