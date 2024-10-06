package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

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
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        val løsning = packet["@løsning.$behov"]
        mediator.løsning(
            hendelseId,
            contextId,
            packet["@id"].asUUID(),
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
