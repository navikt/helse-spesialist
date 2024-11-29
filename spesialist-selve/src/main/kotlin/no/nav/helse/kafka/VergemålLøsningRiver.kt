package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.modell.vergemal.VergemålOgFremtidsfullmakt
import org.slf4j.LoggerFactory

internal class VergemålLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("Vergemål"))
            it.requireKey("fødselsnummer", "hendelseId", "contextId")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.require("@opprettet") { node -> node.asLocalDateTime() }
            it.requireKey("@løsning.Vergemål")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        sikkerLogg.info("Mottok melding Vergemål:\n{}", packet.toJson())
        val contextId = packet["contextId"].asUUID()
        val hendelseId = packet["hendelseId"].asUUID()

        val vergemålNode = packet["@løsning.Vergemål"]
        val harVergemål = !vergemålNode["vergemål"].isEmpty
        val harFremtidsfullmakter = !vergemålNode["fremtidsfullmakter"].isEmpty

        val vergemålOgFremtidsfullmakt =
            VergemålOgFremtidsfullmakt(
                harVergemål = harVergemål,
                harFremtidsfullmakter = harFremtidsfullmakter,
            )

        val vergemålLøsning =
            Vergemålløsning(
                vergemålOgFremtidsfullmakt = vergemålOgFremtidsfullmakt,
            )

        meldingMediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = vergemålLøsning,
            context = context,
        )
    }
}
