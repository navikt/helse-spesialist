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

class VergemålLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
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
        val vergemålNode = packet["@løsning.Vergemål"]

        val vergemålOgFremtidsfullmakt =
            VergemålOgFremtidsfullmakt(
                harVergemål = !vergemålNode["vergemål"].isEmpty,
                harFremtidsfullmakter = !vergemålNode["fremtidsfullmakter"].isEmpty,
            )

        val vergemålLøsning =
            Vergemålløsning(
                vergemålOgFremtidsfullmakt = vergemålOgFremtidsfullmakt,
            )

        meldingMediator.løsning(
            hendelseId = packet["hendelseId"].asUUID(),
            contextId = packet["contextId"].asUUID(),
            behovId = packet["@id"].asUUID(),
            løsning = vergemålLøsning,
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context = context),
        )
    }
}
