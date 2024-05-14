package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.UUID

internal class MidnattRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    override fun validations() =
        River.PacketValidation {
            it.demandAny("@event_name", listOf("midnatt", "slett_gamle_dokumenter_spesialist"))
            it.requireKey("@id")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        logg.error("Forstod ikke midnatt:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        logg.info("Mottok melding midnatt , {}", kv("hendelseId", hendelseId))

        val antallSlettet = mediator.slettGamleDokumenter()
        logg.info("Slettet $antallSlettet dokumenter")
    }
}
