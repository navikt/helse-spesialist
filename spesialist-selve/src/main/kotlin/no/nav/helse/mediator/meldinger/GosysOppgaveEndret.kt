package no.nav.helse.mediator.meldinger

import java.io.File
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.erProd
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
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.oppgave.SjekkAtOppgaveFortsattErÅpenCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
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
    generasjonRepository: GenerasjonRepository,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    oppgaveMediator: OppgaveMediator,
    oppgaveDao: OppgaveDao,
    utbetalingDao: UtbetalingDao,
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    private val utbetaling = utbetalingDao.utbetalingFor(gosysOppgaveEndretCommandData.utbetalingId)

    override val commands: List<Command> = listOf(
        ÅpneGosysOppgaverCommand(
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            warningDao = warningDao,
            varselRepository = varselRepository,
            generasjonRepository = generasjonRepository,
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
            oppgaveMediator = oppgaveMediator,
            utbetaling = utbetaling,
            periodetype = gosysOppgaveEndretCommandData.periodetype,
        )
    )

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator,
        private val oppgaveDao: OppgaveDao,
        private val tildelingDao: TildelingDao,
        private val personDao: PersonDao
    ) : no.nav.helse.rapids_rivers.River.PacketListener {

        private val ignorerliste: Set<String> by lazy {
            if (erProd()) File("/var/run/configmaps/ignorere-oppgave-endret.csv").readText().split(",").toSet()
            else emptySet()
        }
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "gosys_oppgave_endret")
                        it.requireKey("@id", "@opprettet", "fødselsnummer")
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerlogg.error("Forstod ikke gosys_oppgave_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val aktørId = personDao.finnAktørId(fødselsnummer) ?: kotlin.run {
                sikkerlogg.info("Finner ikke aktørid for person. Kjenner ikke til person med {}", kv("fødselsnummer", fødselsnummer))
                return
            }
            if (ignorerliste.contains(fødselsnummer)) {
                sikkerlogg.warn("Ignorerer gosys_oppgave_endret for person $fødselsnummer")
            return
            }
            sikkerlogg.info("gosys_oppgave_endret for fnr {}", fødselsnummer)

            oppgaveDao.finnOppgaveId(fødselsnummer)?.also { oppgaveId ->
                sikkerlogg.info("Fant en oppgave for {}: {}", fødselsnummer, oppgaveId)
                val commandData = oppgaveDao.gosysOppgaveEndretCommandData(oppgaveId)
                if (commandData == null) {
                    sikkerlogg.info("Fant ikke commandData for {} og {}", fødselsnummer, oppgaveId)
                    return
                }

                val tildeling = tildelingDao.tildelingForOppgave(oppgaveId)
                if (tildeling != null) {
                    sikkerlogg.info("Fant tildeling for {}, {}: {}", fødselsnummer, oppgaveId, tildeling)
                    return
                }

                sikkerlogg.info("Har oppgave til_godkjenning, commandData og ingen tildeling for fnr $fødselsnummer og vedtaksperiodeId ${commandData.vedtaksperiodeId}")
                mediator.gosysOppgaveEndret(id, fødselsnummer, aktørId, packet.toJson(), context)
            } ?: sikkerlogg.info("Ingen åpne oppgaver for {}", fødselsnummer)
        }

    }

}
