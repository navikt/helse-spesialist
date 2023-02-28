package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class VedtakFattet(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String,
    private val generasjonRepository: GenerasjonRepository
) : Hendelse {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : PacketListener {

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtak_fattet")
                    it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerlogg.error("Forstod ikke vedtak_fattet:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerlogg.info("Mottok melding om vedtak fattet")

            val fødselsnummer = packet["fødselsnummer"].asText()
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val id = UUID.fromString(packet["@id"].asText())
            mediator.vedtakFattet(id, fødselsnummer, vedtaksperiodeId, packet.toJson(), context)
        }
    }

    override fun execute(context: CommandContext): Boolean {
        try {
            val generasjon = generasjonRepository.sisteFor(vedtaksperiodeId)
            generasjon.håndterVedtakFattet(id)
        } catch (e: IllegalStateException) {
            sikkerlogg.info(
                "Finner ikke noen generasjon for {}, forsøkt låst av {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("hendelseId", id),
            )
        }

        return true
    }
}
