package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.kommando.AvbrytContextCommand
import no.nav.helse.modell.kommando.AvbrytOppgaveCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class AvbrytSaksbehandling(
    override val id: UUID,
    private val fødselsnummer: String,
    vedtaksperiodeId: UUID,
    commandContextDao: CommandContextDao,
    private val json: String,
    oppgaveMediator: OppgaveMediator
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
        AvbrytContextCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            commandContextDao = commandContextDao
        ),
        AvbrytOppgaveCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveMediator = oppgaveMediator
        )
    )

    internal class AvbrytSaksbehandlingRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : River.PacketListener {
        private val log = LoggerFactory.getLogger(this::class.java)

        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireValue("@event_name", "avbryt_saksbehandling")
                    it.requireKey(
                        "@id", "fødselsnummer", "vedtaksperiodeId"
                    )
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            log.info("Avbryter saksbehandling på vedtaksperiode: ${packet["vedtaksperiodeId"].asText()}")
            mediator.avbrytSaksbehandling(packet, context)
        }
    }
}
