package no.nav.helse.mediator

import no.nav.helse.FeatureToggles
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
import no.nav.helse.modell.kommando.LøsGodkjenningsbehov
import no.nav.helse.modell.kommando.OpprettMinimalArbeidsgiverCommand
import no.nav.helse.modell.kommando.OpprettMinimalPersonCommand
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.kommando.TilbakedateringGodkjentCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.EndretEgenAnsattStatusCommand
import no.nav.helse.modell.person.KlargjørTilgangsrelaterteDataCommand
import no.nav.helse.modell.person.OppdaterPersondataCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
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
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.registrerTidsbrukForGodkjenningsbehov
import no.nav.helse.registrerTidsbrukForHendelse
import org.slf4j.LoggerFactory
import java.util.UUID

typealias Kommandostarter = Personmelding.(Kommandofabrikk.() -> Command?) -> Unit

class Kommandofabrikk(
    oppgaveService: () -> OppgaveService,
    private val godkjenningMediator: GodkjenningMediator,
    private val subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
    private val stikkprøver: Stikkprøver,
    @Suppress("unused")
    private val featureToggles: FeatureToggles,
) {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val oppgaveService: OppgaveService by lazy { oppgaveService() }

    internal fun opprettArbeidsgiver(
        organisasjonsnummer: String,
        sessionContext: SessionContext,
    ): OpprettMinimalArbeidsgiverCommand {
        return OpprettMinimalArbeidsgiverCommand(organisasjonsnummer, sessionContext.inntektskilderRepository)
    }

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
        person: Person,
        oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
        sessionContext: SessionContext,
    ): GosysOppgaveEndretCommand {
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)
        val harTildeltOppgave =
            sessionContext.tildelingDao.tildelingForOppgave(oppgaveDataForAutomatisering.oppgaveId) != null
        val godkjenningsbehovData =
            sessionContext.meldingDao
                .finnGodkjenningsbehov(oppgaveDataForAutomatisering.hendelseId)
                .data()

        return GosysOppgaveEndretCommand(
            utbetaling = utbetaling,
            sykefraværstilfelle = person.sykefraværstilfelle(oppgaveDataForAutomatisering.vedtaksperiodeId),
            harTildeltOppgave = harTildeltOppgave,
            oppgavedataForAutomatisering = oppgaveDataForAutomatisering,
            automatisering = transaksjonellAutomatisering(sessionContext),
            åpneGosysOppgaverDao = sessionContext.åpneGosysOppgaverDao,
            oppgaveDao = sessionContext.oppgaveDao,
            oppgaveService = transaksjonellOppgaveService(sessionContext),
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseDao),
            godkjenningsbehov = godkjenningsbehovData,
            automatiseringDao = sessionContext.automatiseringDao,
        )
    }

    internal fun tilbakedateringGodkjent(
        melding: TilbakedateringBehandlet,
        person: Person,
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
        )
    }

    internal fun finnOppgavedata(
        fødselsnummer: String,
        sessionContext: SessionContext,
    ): OppgaveDataForAutomatisering? {
        val oppgaveDao = sessionContext.oppgaveDao
        return oppgaveDao.finnOppgaveId(fødselsnummer)?.let { oppgaveId ->
            sikkerlogg.info("Fant en oppgave for {}: {}", fødselsnummer, oppgaveId)
            val oppgaveDataForAutomatisering = oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)

            if (oppgaveDataForAutomatisering == null) {
                sikkerlogg.info("Fant ikke oppgavedata for {} og {}", fødselsnummer, oppgaveId)
                return null
            } else {
                sikkerlogg.info(
                    "Har aktiv saksbehandleroppgave og oppgavedata for fnr $fødselsnummer og vedtaksperiodeId ${oppgaveDataForAutomatisering.vedtaksperiodeId}",
                )
                return oppgaveDataForAutomatisering
            }
        } ?: kotlin.run {
            sikkerlogg.info("Ingen åpne oppgaver i Speil for {}", fødselsnummer)
            null
        }
    }

    internal fun vedtaksperiodeReberegnet(
        hendelse: VedtaksperiodeReberegnet,
        vedtaksperiode: Vedtaksperiode,
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
        oppgaveDataForAutomatisering: OppgaveDataForAutomatisering?,
        sessionContext: SessionContext,
    ): AdressebeskyttelseEndretCommand {
        val godkjenningsbehovData =
            oppgaveDataForAutomatisering
                ?.let {
                    sessionContext.meldingDao.finnGodkjenningsbehov(it.hendelseId)
                }?.data()
        val utbetaling = godkjenningsbehovData?.let { sessionContext.utbetalingDao.hentUtbetaling(it.utbetalingId) }
        return AdressebeskyttelseEndretCommand(
            fødselsnummer = melding.fødselsnummer(),
            personDao = sessionContext.personDao,
            oppgaveDao = sessionContext.oppgaveDao,
            godkjenningMediator = GodkjenningMediator(sessionContext.opptegnelseDao),
            godkjenningsbehov = godkjenningsbehovData,
            utbetaling = utbetaling,
        )
    }

    internal fun oppdaterPersondata(
        hendelse: Personmelding,
        sessionContext: SessionContext,
    ): OppdaterPersondataCommand =
        OppdaterPersondataCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            førsteKjenteDagFinner = {
                sessionContext.vedtaksperiodeRepository.førsteKjenteDag(
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
            oppgaveService = transaksjonellOppgaveService(sessionContext),
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

    internal fun løsGodkjenningsbehov(
        melding: Saksbehandlerløsning,
        person: Person,
        sessionContext: SessionContext,
    ): LøsGodkjenningsbehov {
        val godkjenningsbehov = sessionContext.meldingDao.finnGodkjenningsbehov(melding.godkjenningsbehovhendelseId)
        val sykefraværstilfelle = person.sykefraværstilfelle(godkjenningsbehov.vedtaksperiodeId())
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(godkjenningsbehov.data().utbetalingId)
        return LøsGodkjenningsbehov(
            utbetaling = utbetaling,
            sykefraværstilfelle = sykefraværstilfelle,
            godkjent = melding.godkjent,
            godkjenttidspunkt = melding.godkjenttidspunkt,
            ident = melding.ident,
            epostadresse = melding.epostadresse,
            årsak = melding.årsak,
            begrunnelser = melding.begrunnelser,
            kommentar = melding.kommentar,
            saksbehandleroverstyringer = melding.saksbehandleroverstyringer,
            saksbehandler = melding.saksbehandler,
            beslutter = melding.beslutter,
            godkjenningMediator = godkjenningMediator,
            godkjenningsbehovData = godkjenningsbehov.data(),
        )
    }

    internal fun godkjenningsbehov(
        godkjenningsbehovData: GodkjenningsbehovData,
        person: Person,
        sessionContext: SessionContext,
    ): GodkjenningsbehovCommand {
        val utbetaling = sessionContext.utbetalingDao.hentUtbetaling(godkjenningsbehovData.utbetalingId)
        val førsteKjenteDagFinner = {
            sessionContext.vedtaksperiodeRepository.førsteKjenteDag(
                godkjenningsbehovData.fødselsnummer,
            )
        }
        return GodkjenningsbehovCommand(
            behovData = godkjenningsbehovData,
            utbetaling = utbetaling,
            førsteKjenteDagFinner = førsteKjenteDagFinner,
            automatisering = transaksjonellAutomatisering(sessionContext),
            vedtakDao = sessionContext.vedtakDao,
            commandContextDao = sessionContext.commandContextDao,
            personDao = sessionContext.personDao,
            inntektskilderRepository = sessionContext.inntektskilderRepository,
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
        )
    }

    // Kanskje prøve å få håndtering av søknad inn i samme flyt som andre kommandokjeder
    fun iverksettSøknadSendt(
        melding: SøknadSendt,
        commandContextObservers: CommandContextObserver,
        sessionContext: SessionContext,
    ) {
        iverksett(
            command = OpprettMinimalPersonCommand(melding.fødselsnummer(), melding.aktørId, sessionContext.personDao),
            meldingId = melding.id,
            commandContext = nyContext(melding.id, sessionContext.commandContextDao),
            commandContextObservers = setOf(commandContextObservers),
            commandContextDao = sessionContext.commandContextDao,
            metrikkDao = sessionContext.metrikkDao,
        )
    }

    private fun nyContext(
        meldingId: UUID,
        transactionalCommandContextDao: CommandContextDao,
    ) = CommandContext(UUID.randomUUID()).apply {
        opprett(transactionalCommandContextDao, meldingId)
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

    private fun transaksjonellOppgaveService(sessionContext: SessionContext): OppgaveService =
        oppgaveService.nyOppgaveService(sessionContext)

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
                    logg.info(
                        "Kommando(er) for ${command.name} er utført ferdig. Det tok ca {}ms å kjøre hele kommandokjeden",
                        kjøretid,
                    )
                } else {
                    logg.info("${command.name} er suspendert")
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
