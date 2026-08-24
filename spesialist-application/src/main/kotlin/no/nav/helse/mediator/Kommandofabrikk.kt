package no.nav.helse.mediator

import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.GodkjenningsbehovUtfall
import no.nav.helse.db.MetrikkDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.stikkprøve.Stikkprøver
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommand
import no.nav.helse.modell.kommando.*
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovCommand
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.registrerTidsbrukForGodkjenningsbehov
import no.nav.helse.registrerTidsbrukForHendelse
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.medMdc
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import java.util.*

typealias Kommandostarter = Personmelding.(Kommandofabrikk.() -> Command?) -> Unit

class Kommandofabrikk(
    private val oppgaveServiceProvider: (SessionContext) -> OppgaveService,
    private val subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
    private val stikkprøver: Stikkprøver,
) {
    internal fun gosysOppgaveEndret(
        person: LegacyPerson,
        oppgave: Oppgave?,
        sessionContext: SessionContext,
    ): Command {
        if (oppgave == null) {
            return ikkesuspenderendeCommand("GosysOppgaveEndretCommand") { _, _ -> }
        }
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(oppgave.utbetalingId)
        val harTildeltOppgave = oppgave.tildeltTil != null
        val godkjenningsbehovData =
            sessionContext.meldingDao
                .finnSisteGodkjenningsbehov(oppgave.behandlingId.value)
                ?.data()
                ?: error("Fant ikke godkjenningsbehov")

        return GosysOppgaveEndretCommand(
            utbetaling = utbetaling,
            sykefraværstilfelle = person.sykefraværstilfelle(oppgave.vedtaksperiodeId.value),
            harTildeltOppgave = harTildeltOppgave,
            oppgave = oppgave,
            automatisering = transaksjonellAutomatisering(sessionContext),
            oppgaveDao = sessionContext.oppgaveDao,
            oppgaveService = oppgaveServiceProvider(sessionContext),
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseRepository),
            godkjenningsbehov = godkjenningsbehovData,
        )
    }

    internal fun tilbakedateringGodkjent(
        melding: TilbakedateringBehandlet,
        person: LegacyPerson,
        oppgave: Oppgave,
        sessionContext: SessionContext,
    ): TilbakedateringGodkjentCommand {
        val godkjenningsbehovData =
            sessionContext.meldingDao.finnSisteGodkjenningsbehov(oppgave.behandlingId.value)?.data()
                ?: error("Fant ikke godkjenningsbehov")
        val sykefraværstilfelle = person.sykefraværstilfelle(godkjenningsbehovData.vedtaksperiodeId)
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(godkjenningsbehovData.utbetalingId)

        return TilbakedateringGodkjentCommand(
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
            automatisering = transaksjonellAutomatisering(sessionContext),
            oppgave = oppgave,
            oppgaveService = oppgaveServiceProvider(sessionContext),
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseRepository),
            søknadsperioder = melding.perioder,
            godkjenningsbehov = godkjenningsbehovData,
        )
    }

    internal fun godkjenningsbehov(
        godkjenningsbehovData: GodkjenningsbehovData,
        person: LegacyPerson,
        sessionContext: SessionContext,
    ): GodkjenningsbehovCommand {
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(godkjenningsbehovData.utbetalingId)
        return GodkjenningsbehovCommand(
            godkjenningsbehovData = godkjenningsbehovData,
            utbetaling = utbetaling,
            automatisering = transaksjonellAutomatisering(sessionContext),
            personDao = sessionContext.personDao,
            vergemålDao = sessionContext.vergemålDao,
            risikovurderingDao = sessionContext.risikovurderingDao,
            periodehistorikkDao = sessionContext.periodehistorikkDao,
            totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
            oppgaveService = oppgaveServiceProvider(sessionContext),
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseRepository),
            person = person,
        )
    }

    fun lagKommandostarter(
        commandContextObservers: Set<CommandContextObserver>,
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Kommandostarter =
        { kommandooppretter ->
            val transactionalCommandContextDao = sessionContext.commandContextDao
            val melding = this
            this@Kommandofabrikk.kommandooppretter()?.let { command ->
                iverksett(
                    command = command,
                    meldingId = melding.id,
                    commandContext = commandContext,
                    commandContextObservers = commandContextObservers,
                    commandContextDao = transactionalCommandContextDao,
                    metrikkDao = sessionContext.metrikkDao,
                    sessionContext = sessionContext,
                    outbox = outbox,
                )
            }
        }

    private fun transaksjonellAutomatisering(sessionContext: SessionContext): Automatisering =
        Automatisering.Factory.automatisering(
            sessionContext,
            subsumsjonsmelderProvider,
            stikkprøver,
        )

    private fun iverksett(
        command: Command,
        meldingId: UUID,
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
        commandContextObservers: Collection<CommandContextObserver>,
        commandContextDao: CommandContextDao,
        metrikkDao: MetrikkDao,
    ) {
        commandContextObservers.forEach { commandContext.nyObserver(it) }
        val contextId = commandContext.id()
        medMdc(MdcKey.CONTEXT_ID to contextId.toString()) {
            try {
                if (commandContext.utfør(commandContextDao, sessionContext, outbox, meldingId, command)) {
                    val kjøretid = commandContextDao.tidsbrukForContext(contextId)
                    metrikker(command.name, kjøretid, contextId, metrikkDao)
                    loggInfo("Kommando(er) for ${command.name} er utført ferdig")
                    loggDebug("Det tok ca ${kjøretid}ms å kjøre hele kommandokjeden")
                } else {
                    loggInfo("${command.name} er suspendert")
                }
            } finally {
                commandContextObservers.forEach { commandContext.avregistrerObserver(it) }
            }
        }
    }

    private fun metrikker(
        hendelsenavn: String,
        kjøretidMs: Int,
        contextId: UUID,
        metrikkDao: MetrikkDao,
    ) {
        if (hendelsenavn == GodkjenningsbehovCommand::class.simpleName) {
            val utfall: GodkjenningsbehovUtfall = metrikkDao.finnUtfallForGodkjenningsbehov(contextId)
            registrerTidsbrukForGodkjenningsbehov(utfall, kjøretidMs)
        }
        registrerTidsbrukForHendelse(hendelsenavn, kjøretidMs)
    }
}
