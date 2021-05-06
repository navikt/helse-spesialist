package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.ReserverPersonHvisTildeltCommand
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.tildeling.TildelingDao
import org.slf4j.LoggerFactory
import java.util.*

internal class VedtaksperiodeReberegnet(
    override val id: UUID,
    private val fødselsnummer: String,
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    private val json: String,
    oppgaveMediator: OppgaveMediator,
    reservasjonDao: ReservasjonDao,
    tildelingDao: TildelingDao,
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
        ReserverPersonHvisTildeltCommand(fødselsnummer, reservasjonDao, tildelingDao),
        AvbrytCommand(vedtaksperiodeId, commandContextDao, oppgaveMediator)
    )

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : no.nav.helse.rapids_rivers.River.PacketListener {
        private val log = LoggerFactory.getLogger(this::class.java)

        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireValue("@event_name", "vedtaksperiode_reberegnet")
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
