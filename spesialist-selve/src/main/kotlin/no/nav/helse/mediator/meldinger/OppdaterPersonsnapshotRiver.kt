package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class OppdaterPersonsnapshotRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
) : River.PacketListener {
    private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "oppdater_personsnapshot")
                it.requireKey("@id", "fødselsnummer")
            }
        }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("Forstod ikke oppdater_personsnapshot:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val id = UUID.fromString(packet["@id"].asText())
        val fødselsnummer = packet["fødselsnummer"].asText()
        sikkerlogg.info(
            "Mottok forespørsel om å oppdatere personsnapshot på {}, {}",
            StructuredArguments.keyValue("fødselsnummer", fødselsnummer),
            StructuredArguments.keyValue("eventId", id),
        )
        mediator.mottaMelding(OppdaterPersonsnapshot(packet), context)
    }
}
