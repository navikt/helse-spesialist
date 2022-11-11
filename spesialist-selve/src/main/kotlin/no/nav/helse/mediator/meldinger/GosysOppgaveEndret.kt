package no.nav.helse.mediator.meldinger

import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringForEksisterendeOppgaveCommand
import no.nav.helse.modell.automatisering.SettTidligereAutomatiseringInaktivCommand
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommandData
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

internal class GosysOppgaveEndret(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    private val json: String,
    gosysOppgaveEndretCommandData: GosysOppgaveEndretCommandData,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    warningDao: WarningDao,
    varselRepository: VarselRepository,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    oppgaveMediator: OppgaveMediator,
    oppgaveDao: OppgaveDao
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
        ÅpneGosysOppgaverCommand(
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            warningDao = warningDao,
            varselRepository = varselRepository,
            vedtaksperiodeId = gosysOppgaveEndretCommandData.vedtaksperiodeId
        ),
        SjekkAtOppgaveFortsattErÅpenCommand(fødselsnummer = fødselsnummer, oppgaveDao = oppgaveDao),
        SettTidligereAutomatiseringInaktivCommand(
            vedtaksperiodeId = gosysOppgaveEndretCommandData.vedtaksperiodeId,
            hendelseId = gosysOppgaveEndretCommandData.hendelseId,
            automatisering = automatisering,
        ),
        AutomatiseringForEksisterendeOppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = gosysOppgaveEndretCommandData.vedtaksperiodeId,
            utbetalingId = gosysOppgaveEndretCommandData.utbetalingId,
            hendelseId = gosysOppgaveEndretCommandData.hendelseId,
            automatisering = automatisering,
            godkjenningsbehovJson = gosysOppgaveEndretCommandData.godkjenningsbehovJson,
            godkjenningMediator = godkjenningMediator,
            oppgaveMediator = oppgaveMediator
        )
    )

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator,
        private val oppgaveDao: OppgaveDao,
        private val tildelingDao: TildelingDao
    ) : no.nav.helse.rapids_rivers.River.PacketListener {

        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "gosys_oppgave_endret")
                        it.requireKey("@id", "@opprettet", "fødselsnummer", "aktørId")
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("Forstod ikke gosys_oppgave_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val fødselsnummer = packet["fødselsnummer"].asText()
            sikkerLog.info("gosys_oppgave_endret for fnr {}", fødselsnummer)

            oppgaveDao.finnOppgaveId(fødselsnummer)?.also { oppgaveId ->
                sikkerLog.info("Fant en oppgave for {}: {}", fødselsnummer, oppgaveId)
                val commandData = oppgaveDao.gosysOppgaveEndretCommandData(oppgaveId)
                if (commandData == null) {
                    sikkerLog.info("Fant ikke commandData for {} og {}", fødselsnummer, oppgaveId)
                    return
                }

                val tildeling = tildelingDao.tildelingForOppgave(oppgaveId)
                if (tildeling != null) {
                    sikkerLog.info("Fant tildeling for {}, {}: {}", fødselsnummer, oppgaveId, tildeling)
                    return
                }

                sikkerLog.info("Har oppgave til_godkjenning, commandData og ingen tildeling for fnr $fødselsnummer og vedtaksperiodeId ${commandData.vedtaksperiodeId}")
                mediator.gosysOppgaveEndret(packet, context)
            } ?: sikkerLog.info("Ingen åpne oppgaver for {}", fødselsnummer)
        }
    }

}
