package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class PåminnetGodkjenningsbehovRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
) : River.PacketListener {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAll("@behov", listOf("Godkjenning"))
                it.rejectKey("@løsning")
                it.requireKey(
                    "@id", "fødselsnummer", "vedtaksperiodeId", "utbetalingId"
                )
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        logg.info(
            "Sjekker om snapshot er utdatert for godkjenningsbehov med {}",
            keyValue("hendelseId", hendelseId)
        )
        mediator.påminnetGodkjenningsbehov(
            message = packet,
            id = hendelseId,
            fødselsnummer = packet["fødselsnummer"].asText(),
            vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
            utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
            context = context
        )
    }
}