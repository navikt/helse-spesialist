package no.nav.helse.mediator.meldinger
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.OppgaveDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppgaveMakstidCommand
import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class OppgaveMakstidPåminnelse(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    private val oppgaveId: Long,
    private val oppgaveDao: OppgaveDao,
    hendelseDao: HendelseDao,
    godkjenningMediator: GodkjenningMediator,
    oppgaveMediator: OppgaveMediator
): Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppgaveMakstidCommand(oppgaveId, fødselsnummer, vedtaksperiodeId(), oppgaveDao, hendelseDao, godkjenningMediator, oppgaveMediator)
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
    override fun vedtaksperiodeId() = oppgaveDao.finnVedtaksperiodeId(oppgaveId)

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : PacketListener {

        private val log = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "påminnelse_oppgave_makstid")
                    it.requireKey("@id", "fødselsnummer", "oppgaveId")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke påminnelse_oppgave_makstid:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val oppgaveId = packet["oppgaveId"].asLong()
            val id = UUID.fromString(packet["@id"].asText())
            log.info(
                "Mottok påminnelse_oppgave_makstid endret {}, {}",
                keyValue("oppgaveId", oppgaveId),
                keyValue("eventId", id)
            )
            mediator.påminnelseOppgaveMakstid(packet, context)
        }
    }
}
