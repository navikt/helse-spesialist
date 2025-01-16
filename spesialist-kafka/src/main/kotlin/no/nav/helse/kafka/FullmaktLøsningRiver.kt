package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.isSameOrAfter
import no.nav.helse.mediator.meldinger.løsninger.isSameOrBefore
import org.slf4j.LoggerFactory
import java.time.LocalDate

class FullmaktLøsningRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("Fullmakt"))
            it.requireKey("fødselsnummer", "contextId", "hendelseId")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
            it.require("@opprettet") { node -> node.asLocalDateTime() }
            it.requireArray("@løsning.Fullmakt") {
                interestedIn("gyldigFraOgMed", "gyldigTilOgMed")
            }
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        sikkerlogg.info("Mottok melding Fullmakt:\n{}", packet.toJson())
        val contextId = packet["contextId"].asUUID()
        val hendelseId = packet["hendelseId"].asUUID()

        val nå = LocalDate.now()
        val harFullmakt =
            packet["@løsning.Fullmakt"].any { fullmaktNode ->
                fullmaktNode["gyldigFraOgMed"].asLocalDate().isSameOrBefore(nå) &&
                    fullmaktNode["gyldigTilOgMed"].asOptionalLocalDate()?.isSameOrAfter(nå) ?: true
            }

        val fullmaktløsning =
            Fullmaktløsning(
                harFullmakt = harFullmakt,
            )

        meldingMediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = fullmaktløsning,
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context = context),
        )
    }
}
