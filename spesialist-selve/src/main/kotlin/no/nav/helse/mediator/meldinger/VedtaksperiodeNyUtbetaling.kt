package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.OpprettKoblingTilGenerasjonCommand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class VedtaksperiodeNyUtbetaling(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    utbetalingId: UUID,
    private val json: String,
    utbetalingDao: UtbetalingDao,
    generasjonRepository: GenerasjonRepository,
    varselRepository: VarselRepository,
) : Hendelse, MacroCommand() {

    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override val commands: List<Command> = listOf(
        OpprettKoblingTilUtbetalingCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            utbetalingDao = utbetalingDao,
        ),
        OpprettKoblingTilGenerasjonCommand(
            hendelseId = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            generasjonRepository = generasjonRepository,
        )
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
    override fun toJson(): String = json

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : PacketListener {

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_ny_utbetaling")
                    it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId", "utbetalingId")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val fødselsnummer = packet["fødselsnummer"].asText()
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
            val id = UUID.fromString(packet["@id"].asText())

            sikkerLogg.info(
                "Mottok melding om vedtaksperiode_ny_utbetaling for {}, {}, {} som følge av melding med {}",
                kv("fødselsnummer", fødselsnummer),
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("utbetalingId", utbetalingId),
                kv("id", id)
            )

            mediator.vedtaksperiodeNyUtbetaling(fødselsnummer, id, vedtaksperiodeId, utbetalingId, packet.toJson(), context)
        }
    }
}
