package no.nav.helse.mediator.meldinger

import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.VedtaksperiodeReberegnetPeriodehistorikk
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import org.slf4j.LoggerFactory

internal class VedtaksperiodeReberegnet(
    override val id: UUID,
    private val fødselsnummer: String,
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    private val json: String,
    oppgaveMediator: OppgaveMediator,
    periodehistorikkDao: PeriodehistorikkDao,
    utbetalingDao: UtbetalingDao,
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
        VedtaksperiodeReberegnetPeriodehistorikk(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingDao = utbetalingDao,
            periodehistorikkDao = periodehistorikkDao
        ),
        AvbrytCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            commandContextDao = commandContextDao,
            oppgaveMediator = oppgaveMediator
        )
    )

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : no.nav.helse.rapids_rivers.River.PacketListener {
        private val log = LoggerFactory.getLogger(this::class.java)

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_endret")
                    it.demand("forrigeTilstand") { node -> check(node.asText().startsWith("AVVENTER_GODKJENNING")) }
                    it.rejectValues("gjeldendeTilstand", listOf("AVSLUTTET", "TIL_UTBETALING", "TIL_INFOTRYGD"))
                    it.requireKey(
                        "@id", "fødselsnummer", "vedtaksperiodeId"
                    )
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            log.info("Avbryter saksbehandling på vedtaksperiode: ${packet["vedtaksperiodeId"].asText()}")
            mediator.avbrytSaksbehandling(packet, context)
        }
    }
}
