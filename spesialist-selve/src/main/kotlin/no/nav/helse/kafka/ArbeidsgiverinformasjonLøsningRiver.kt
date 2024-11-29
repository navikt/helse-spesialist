package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import org.slf4j.LoggerFactory

internal class ArbeidsgiverinformasjonLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val behov = "Arbeidsgiverinformasjon"

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf(behov))
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("contextId", "hendelseId", "@id")
            it.requireKey("@løsning.$behov")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerLog.error("forstod ikke $behov:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
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
