package no.nav.helse.mediator

import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.GodkjenningsbehovUtfall
import no.nav.helse.db.MetrikkDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndretCommand
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommand
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.kommando.TilbakedateringGodkjentCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.EndretEgenAnsattStatusCommand
import no.nav.helse.modell.person.KlargjørTilgangsrelaterteDataCommand
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.person.OppdaterPersondataCommand
import no.nav.helse.modell.person.vedtaksperiode.LegacyVedtaksperiode
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.UtbetalingEndretCommand
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovCommand
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastetCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetalingCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnetCommand
import no.nav.helse.registrerTidsbrukForGodkjenningsbehov
import no.nav.helse.registrerTidsbrukForHendelse
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import java.util.UUID

typealias Kommandostarter = Personmelding.(Kommandofabrikk.() -> Command?) -> Unit

class Kommandofabrikk(
    oppgaveService: () -> OppgaveService,
    private val subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
    private val stikkprøver: Stikkprøver,
) {
    private val oppgaveService: OppgaveService by lazy { oppgaveService() }

    internal fun endretEgenAnsattStatus(
        melding: EndretEgenAnsattStatus,
        sessionContext: SessionContext,
    ): EndretEgenAnsattStatusCommand =
        EndretEgenAnsattStatusCommand(
            fødselsnummer = melding.fødselsnummer(),
            erEgenAnsatt = melding.erEgenAnsatt,
            opprettet = melding.opprettet,
            egenAnsattDao = sessionContext.egenAnsattDao,
            oppgaveService = transaksjonellOppgaveService(sessionContext),
        )

    internal fun gosysOppgaveEndret(
        person: LegacyPerson,
        oppgave: Oppgave,
        sessionContext: SessionContext,
    ): GosysOppgaveEndretCommand {
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(oppgave.utbetalingId)
        val harTildeltOppgave = oppgave.tildeltTil != null
        val godkjenningsbehovData =
            sessionContext.meldingDao
                .finnSisteGodkjenningsbehov(oppgave.behandlingId)
                ?.data()
                ?: error("Fant ikke godkjenningsbehov")

        return GosysOppgaveEndretCommand(
            utbetaling = utbetaling,
            sykefraværstilfelle = person.sykefraværstilfelle(oppgave.vedtaksperiodeId),
            harTildeltOppgave = harTildeltOppgave,
            oppgave = oppgave,
            automatisering = transaksjonellAutomatisering(sessionContext),
            åpneGosysOppgaverDao = sessionContext.åpneGosysOppgaverDao,
            oppgaveDao = sessionContext.oppgaveDao,
            oppgaveService = transaksjonellOppgaveService(sessionContext),
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseDao),
            godkjenningsbehov = godkjenningsbehovData,
            automatiseringDao = sessionContext.automatiseringDao,
            vedtakRepository = sessionContext.vedtakRepository,
        )
    }

    internal fun tilbakedateringGodkjent(
        melding: TilbakedateringBehandlet,
        person: LegacyPerson,
        oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
        sessionContext: SessionContext,
    ): TilbakedateringGodkjentCommand {
        val godkjenningsbehovData =
            sessionContext.meldingDao.finnGodkjenningsbehov(oppgaveDataForAutomatisering.hendelseId).data()
        val sykefraværstilfelle = person.sykefraværstilfelle(godkjenningsbehovData.vedtaksperiodeId)
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(godkjenningsbehovData.utbetalingId)

        return TilbakedateringGodkjentCommand(
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
            automatisering = transaksjonellAutomatisering(sessionContext),
            oppgaveDataForAutomatisering = oppgaveDataForAutomatisering,
            oppgaveService = transaksjonellOppgaveService(sessionContext),
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseDao),
            søknadsperioder = melding.perioder,
            godkjenningsbehov = godkjenningsbehovData,
            automatiseringDao = sessionContext.automatiseringDao,
            vedtakRepository = sessionContext.vedtakRepository,
        )
    }

    internal fun finnOppgavedata(
        fødselsnummer: String,
        sessionContext: SessionContext,
    ): OppgaveDataForAutomatisering? {
        val oppgaveDao = sessionContext.oppgaveDao
        return oppgaveDao.finnOppgaveId(fødselsnummer)?.let { oppgaveId ->
            loggInfo("Fant en oppgave med id: $oppgaveId", "fødselsnummer: $fødselsnummer")
            val oppgaveDataForAutomatisering = oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)

            if (oppgaveDataForAutomatisering == null) {
                loggInfo("Fant ikke oppgavedata med id: $oppgaveId", "fødselsnummer: $fødselsnummer")
                null
            } else {
                loggInfo(
                    melding = "Har aktiv saksbehandleroppgave med id: $oppgaveId, vedtaksperiodeId: ${oppgaveDataForAutomatisering.vedtaksperiodeId}",
                    sikkerloggDetaljer = "fødselsnummer: $fødselsnummer",
                )
                oppgaveDataForAutomatisering
            }
        } ?: kotlin.run {
            loggInfo("Ingen aktive saksbehandleroppgaver funnet for personen", "fødselsnummer: $fødselsnummer")
            null
        }
    }

    internal fun vedtaksperiodeReberegnet(
        hendelse: VedtaksperiodeReberegnet,
        vedtaksperiode: LegacyVedtaksperiode,
        sessionContext: SessionContext,
    ): VedtaksperiodeReberegnetCommand =
        VedtaksperiodeReberegnetCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            vedtaksperiode = vedtaksperiode,
            periodehistorikkDao = sessionContext.periodehistorikkDao,
            commandContextDao = sessionContext.commandContextDao,
            oppgaveService = transaksjonellOppgaveService(sessionContext),
            reservasjonDao = sessionContext.reservasjonDao,
            tildelingDao = sessionContext.tildelingDao,
            totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
        )

    internal fun vedtaksperiodeNyUtbetaling(
        hendelse: VedtaksperiodeNyUtbetaling,
        sessionContext: SessionContext,
    ): VedtaksperiodeNyUtbetalingCommand =
        VedtaksperiodeNyUtbetalingCommand(
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            utbetalingId = hendelse.utbetalingId,
            utbetalingDao = sessionContext.utbetalingDao,
        )

    internal fun adressebeskyttelseEndret(
        melding: AdressebeskyttelseEndret,
        sessionContext: SessionContext,
    ): AdressebeskyttelseEndretCommand {
        val identitetsnummer = Identitetsnummer.fraString(melding.fødselsnummer())
        return AdressebeskyttelseEndretCommand(
            identitetsnummer = identitetsnummer,
            meldingDao = sessionContext.meldingDao,
            personRepository = sessionContext.personRepository,
            oppgaveRepository = sessionContext.oppgaveRepository,
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseDao),
        )
    }

    internal fun oppdaterPersondata(
        hendelse: Personmelding,
        sessionContext: SessionContext,
    ): OppdaterPersondataCommand =
        OppdaterPersondataCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            førsteKjenteDagFinner = {
                sessionContext.legacyVedtaksperiodeRepository.førsteKjenteDag(
                    hendelse.fødselsnummer(),
                )
            },
            personDao = sessionContext.personDao,
            opptegnelseDao = sessionContext.opptegnelseDao,
        )

    internal fun klargjørTilgangsrelaterteData(
        hendelse: Personmelding,
        sessionContext: SessionContext,
    ): KlargjørTilgangsrelaterteDataCommand =
        KlargjørTilgangsrelaterteDataCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            personDao = sessionContext.personDao,
            egenAnsattDao = sessionContext.egenAnsattDao,
            opptegnelseDao = sessionContext.opptegnelseDao,
            personRepository = sessionContext.personRepository,
        )

    internal fun utbetalingEndret(
        hendelse: UtbetalingEndret,
        sessionContext: SessionContext,
    ): UtbetalingEndretCommand =
        UtbetalingEndretCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            organisasjonsnummer = hendelse.organisasjonsnummer,
            utbetalingId = hendelse.utbetalingId,
            utbetalingstype = hendelse.type,
            gjeldendeStatus = hendelse.gjeldendeStatus,
            opprettet = hendelse.opprettet,
            arbeidsgiverOppdrag = hendelse.arbeidsgiverOppdrag,
            personOppdrag = hendelse.personOppdrag,
            arbeidsgiverbeløp = hendelse.arbeidsgiverbeløp,
            personbeløp = hendelse.personbeløp,
            utbetalingDao = sessionContext.utbetalingDao,
            opptegnelseDao = sessionContext.opptegnelseDao,
            reservasjonDao = sessionContext.reservasjonDao,
            tildelingDao = sessionContext.tildelingDao,
            oppgaveService = transaksjonellOppgaveService(sessionContext),
            totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
            json = hendelse.toJson(),
        )

    internal fun vedtaksperiodeForkastet(
        hendelse: VedtaksperiodeForkastet,
        alleForkastedeVedtaksperiodeIder: List<UUID>,
        sessionContext: SessionContext,
    ): VedtaksperiodeForkastetCommand =
        VedtaksperiodeForkastetCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            id = hendelse.id,
            alleForkastedeVedtaksperiodeIder = alleForkastedeVedtaksperiodeIder,
            commandContextDao = sessionContext.commandContextDao,
            oppgaveService = transaksjonellOppgaveService(sessionContext),
            reservasjonDao = sessionContext.reservasjonDao,
            tildelingDao = sessionContext.tildelingDao,
            totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
        )

    internal fun stansAutomatiskBehandling(
        hendelse: StansAutomatiskBehandlingMelding,
        sessionContext: SessionContext,
    ) = ikkesuspenderendeCommand {
        StansAutomatiskBehandlingMediator.Factory
            .stansAutomatiskBehandlingMediator(
                sessionContext,
                subsumsjonsmelderProvider,
            ).håndter(hendelse)
    }

    internal fun godkjenningsbehov(
        godkjenningsbehovData: GodkjenningsbehovData,
        person: LegacyPerson,
        sessionContext: SessionContext,
    ): GodkjenningsbehovCommand {
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(godkjenningsbehovData.utbetalingId)
        val førsteKjenteDagFinner = {
            sessionContext.legacyVedtaksperiodeRepository.førsteKjenteDag(
                godkjenningsbehovData.fødselsnummer,
            )
        }
        return GodkjenningsbehovCommand(
            behovData = godkjenningsbehovData,
            utbetaling = utbetaling,
            førsteKjenteDagFinner = førsteKjenteDagFinner,
            automatisering = transaksjonellAutomatisering(sessionContext),
            vedtakDao = sessionContext.vedtakDao,
            meldingDao = sessionContext.meldingDao,
            commandContextDao = sessionContext.commandContextDao,
            personDao = sessionContext.personDao,
            arbeidsgiverRepository = sessionContext.arbeidsgiverRepository,
            arbeidsforholdDao = sessionContext.arbeidsforholdDao,
            egenAnsattDao = sessionContext.egenAnsattDao,
            utbetalingDao = sessionContext.utbetalingDao,
            vergemålDao = sessionContext.vergemålDao,
            åpneGosysOppgaverDao = sessionContext.åpneGosysOppgaverDao,
            risikovurderingDao = sessionContext.risikovurderingDao,
            påVentDao = sessionContext.påVentDao,
            automatiseringDao = sessionContext.automatiseringDao,
            oppgaveRepository = sessionContext.oppgaveRepository,
            oppgaveDao = sessionContext.oppgaveDao,
            periodehistorikkDao = sessionContext.periodehistorikkDao,
            totrinnsvurderingRepository = sessionContext.totrinnsvurderingRepository,
            avviksvurderingRepository = sessionContext.avviksvurderingRepository,
            opptegnelseDao = sessionContext.opptegnelseDao,
            oppgaveService = transaksjonellOppgaveService(sessionContext),
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseDao),
            person = person,
            tildelingDao = sessionContext.tildelingDao,
            reservasjonDao = sessionContext.reservasjonDao,
            vedtakRepository = sessionContext.vedtakRepository,
            personRepository = sessionContext.personRepository,
        )
    }

    fun lagKommandostarter(
        commandContextObservers: Set<CommandContextObserver>,
        commandContext: CommandContext,
        sessionContext: SessionContext,
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
                )
            }
        }

    private fun transaksjonellOppgaveService(sessionContext: SessionContext): OppgaveService = oppgaveService.nyOppgaveService(sessionContext)

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
        commandContextObservers: Collection<CommandContextObserver>,
        commandContextDao: CommandContextDao,
        metrikkDao: MetrikkDao,
    ) {
        commandContextObservers.forEach { commandContext.nyObserver(it) }
        val contextId = commandContext.id()
        withMDC(
            mapOf("contextId" to contextId.toString()),
        ) {
            try {
                if (commandContext.utfør(commandContextDao, meldingId, command)) {
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
