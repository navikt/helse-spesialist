package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.PåminnelseCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class VedtaksperiodePåminnet(
    override val id: UUID,
    private val fødselsnummer: String,
    vedtaksperiodeId: UUID,
    private val json: String,
    oppgaveMediator: OppgaveMediator,
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
        PåminnelseCommand(vedtaksperiodeId, oppgaveMediator)
    )

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : no.nav.helse.rapids_rivers.River.PacketListener {
        private val log = LoggerFactory.getLogger(this::class.java)

        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireValue("@event_name", "vedtaksperiode_påminnet")
                    it.requireKey(
                        "@id", "fødselsnummer"
                    )
                    it.requireKey(
                        "tilstand", "vedtaksperiodeId"
                    )
                    it.rejectValue("tilstand", "AVVENTER_GODKJENNING")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            log.info("Mottar påminnelse på vedtaksperiode: ${packet["vedtaksperiodeId"].asText()}")
            mediator.fjernGjenliggendeOppgaver(packet, context)
        }
    }
}
