package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.slf4j.LoggerFactory
import java.util.UUID

internal class ArbeidsgiverinformasjonLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val behov = "Arbeidsgiverinformasjon"

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behov")
            it.demandValue("@final", true)
            it.demandAll("@behov", listOf(behov))
            it.interestedIn("fødselsnummer")
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
        if (packet["fødselsnummer"].isMissingOrNull()) {
            sikkerLog.debug("Mottok svar for arbeidsgivernavn\n{}", packet.toJson())
            alternativHåndtering(løsning, contextId, hendelseId)
            return
        }
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

    private fun alternativHåndtering(
        løsning: JsonNode,
        contextId: UUID,
        hendelseId: UUID,
    ) {
        val navnOgBransjer =
            løsning.map { arbeidsgiver ->
                Triple(
                    arbeidsgiver.path("orgnummer").asText(),
                    arbeidsgiver.path("navn").asText(),
                    arbeidsgiver.path("bransjer").map { it.asText() },
                )
            }.toSet()
        mediator.oppdaterInntektskilder(
            navnOgBransjer,
            contextId,
            hendelseId,
        )
    }
}
