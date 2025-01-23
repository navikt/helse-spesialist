package no.nav.helse.mediator

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.Repositories
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndretCommand
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommand
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.LøsGodkjenningsbehov
import no.nav.helse.modell.kommando.OverstyringIgangsattCommand
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.kommando.TilbakedateringGodkjentCommand
import no.nav.helse.modell.kommando.ikkesuspenderendeCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.EndretEgenAnsattStatusCommand
import no.nav.helse.modell.person.KlargjørTilgangsrelaterteDataCommand
import no.nav.helse.modell.person.OppdaterPersondataCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.SøknadSendtCommand
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMelding
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.UtbetalingEndretCommand
import no.nav.helse.modell.vedtaksperiode.GenerasjonService
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovCommand
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastetCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetalingCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnetCommand
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto
import no.nav.helse.registrerTidsbrukForGodkjenningsbehov
import no.nav.helse.registrerTidsbrukForHendelse
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

typealias Kommandostarter = Personmelding.(Kommandofabrikk.() -> Command?) -> Unit

class Kommandofabrikk(
    private val dataSource: DataSource,
    private val repositories: Repositories,
    oppgaveService: () -> OppgaveService,
    private val godkjenningMediator: GodkjenningMediator,
    private val generasjonService: GenerasjonService = GenerasjonService(repositories),
    private val subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
    private val stikkprøver: Stikkprøver,
) {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val oppgaveService: OppgaveService by lazy { oppgaveService() }

    internal fun endretEgenAnsattStatus(
        melding: EndretEgenAnsattStatus,
        transactionalSession: TransactionalSession,
    ): EndretEgenAnsattStatusCommand =
        EndretEgenAnsattStatusCommand(
            fødselsnummer = melding.fødselsnummer(),
            erEgenAnsatt = melding.erEgenAnsatt,
            opprettet = melding.opprettet,
            egenAnsattDao = repositories.withSessionContext(transactionalSession).egenAnsattDao,
            oppgaveService = transaksjonellOppgaveService(transactionalSession),
        )

    internal fun gosysOppgaveEndret(
        person: Person,
        oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
        transactionalSession: TransactionalSession,
    ): GosysOppgaveEndretCommand {
        val utbetaling = UtbetalingDao(transactionalSession).hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)
        val harTildeltOppgave =
            repositories.withSessionContext(transactionalSession).tildelingDao.tildelingForOppgave(oppgaveDataForAutomatisering.oppgaveId) != null
        val godkjenningsbehovData =
            MeldingDao(transactionalSession)
                .finnGodkjenningsbehov(oppgaveDataForAutomatisering.hendelseId)
                .data()

        return GosysOppgaveEndretCommand(
            utbetaling = utbetaling,
            sykefraværstilfelle = person.sykefraværstilfelle(oppgaveDataForAutomatisering.vedtaksperiodeId),
            harTildeltOppgave = harTildeltOppgave,
            oppgavedataForAutomatisering = oppgaveDataForAutomatisering,
            automatisering = transaksjonellAutomatisering(transactionalSession),
            åpneGosysOppgaverDao = repositories.withSessionContext(transactionalSession).åpneGosysOppgaverDao,
            oppgaveDao = repositories.withSessionContext(transactionalSession).oppgaveDao,
            oppgaveService = transaksjonellOppgaveService(transactionalSession),
            godkjenningMediator = GodkjenningMediator(repositories.withSessionContext(transactionalSession).opptegnelseRepository),
            godkjenningsbehov = godkjenningsbehovData,
            automatiseringDao = repositories.withSessionContext(transactionalSession).automatiseringDao,
        )
    }

    internal fun tilbakedateringGodkjent(
        melding: TilbakedateringBehandlet,
        person: Person,
        oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
        transactionalSession: TransactionalSession,
    ): TilbakedateringGodkjentCommand {
        val godkjenningsbehovData =
            MeldingDao(transactionalSession).finnGodkjenningsbehov(oppgaveDataForAutomatisering.hendelseId).data()
        val sykefraværstilfelle = person.sykefraværstilfelle(godkjenningsbehovData.vedtaksperiodeId)
        val utbetaling = UtbetalingDao(transactionalSession).hentUtbetaling(godkjenningsbehovData.utbetalingId)

        return TilbakedateringGodkjentCommand(
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
            automatisering = transaksjonellAutomatisering(transactionalSession),
            oppgaveDataForAutomatisering = oppgaveDataForAutomatisering,
            oppgaveService = transaksjonellOppgaveService(transactionalSession),
            godkjenningMediator = GodkjenningMediator(repositories.withSessionContext(transactionalSession).opptegnelseRepository),
            søknadsperioder = melding.perioder,
            godkjenningsbehov = godkjenningsbehovData,
            automatiseringDao = repositories.withSessionContext(transactionalSession).automatiseringDao,
        )
    }

    internal fun finnOppgavedata(
        fødselsnummer: String,
        session: Session,
    ): OppgaveDataForAutomatisering? {
        val oppgaveDao = repositories.withSessionContext(session).oppgaveDao
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
        session: TransactionalSession,
    ): VedtaksperiodeReberegnetCommand =
        VedtaksperiodeReberegnetCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            vedtaksperiode = vedtaksperiode,
            periodehistorikkDao = repositories.withSessionContext(session).periodehistorikkDao,
            commandContextDao = repositories.withSessionContext(session).commandContextDao,
            oppgaveService = transaksjonellOppgaveService(session),
            reservasjonDao = repositories.withSessionContext(session).reservasjonDao,
            tildelingDao = repositories.withSessionContext(session).tildelingDao,
            oppgaveDao = repositories.withSessionContext(session).oppgaveDao,
            totrinnsvurderingService = lagTotrinnsvurderingService(repositories.withSessionContext(session)),
        )

    internal fun vedtaksperiodeNyUtbetaling(
        hendelse: VedtaksperiodeNyUtbetaling,
        transactionalSession: TransactionalSession,
    ): VedtaksperiodeNyUtbetalingCommand =
        VedtaksperiodeNyUtbetalingCommand(
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            utbetalingId = hendelse.utbetalingId,
            utbetalingRepository = UtbetalingDao(transactionalSession),
        )

    fun søknadSendt(
        hendelse: SøknadSendt,
        transactionalSession: TransactionalSession,
    ): SøknadSendtCommand =
        SøknadSendtCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            aktørId = hendelse.aktørId,
            organisasjonsnummer = hendelse.organisasjonsnummer,
            personRepository = PersonDao(transactionalSession),
            inntektskilderRepository = repositories.withSessionContext(transactionalSession).inntektskilderRepository,
        )

    internal fun adressebeskyttelseEndret(
        melding: AdressebeskyttelseEndret,
        oppgaveDataForAutomatisering: OppgaveDataForAutomatisering?,
        transactionalSession: TransactionalSession,
    ): AdressebeskyttelseEndretCommand {
        val godkjenningsbehovData =
            oppgaveDataForAutomatisering
                ?.let {
                    MeldingDao(transactionalSession).finnGodkjenningsbehov(it.hendelseId)
                }?.data()
        val utbetaling = godkjenningsbehovData?.let { UtbetalingDao(transactionalSession).hentUtbetaling(it.utbetalingId) }
        return AdressebeskyttelseEndretCommand(
            fødselsnummer = melding.fødselsnummer(),
            personRepository = PersonDao(transactionalSession),
            oppgaveDao = repositories.withSessionContext(transactionalSession).oppgaveDao,
            godkjenningMediator = GodkjenningMediator(repositories.withSessionContext(transactionalSession).opptegnelseRepository),
            godkjenningsbehov = godkjenningsbehovData,
            utbetaling = utbetaling,
        )
    }

    internal fun oppdaterPersondata(
        hendelse: Personmelding,
        transactionalSession: TransactionalSession,
    ): OppdaterPersondataCommand =
        OppdaterPersondataCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            førsteKjenteDagFinner = { generasjonService.førsteKjenteDag(hendelse.fødselsnummer()) },
            personRepository = PersonDao(transactionalSession),
            opptegnelseRepository = repositories.withSessionContext(transactionalSession).opptegnelseRepository,
        )

    internal fun klargjørTilgangsrelaterteData(
        hendelse: Personmelding,
        transactionalSession: TransactionalSession,
    ): KlargjørTilgangsrelaterteDataCommand =
        KlargjørTilgangsrelaterteDataCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            personRepository = PersonDao(transactionalSession),
            egenAnsattDao = repositories.withSessionContext(transactionalSession).egenAnsattDao,
            opptegnelseRepository = repositories.withSessionContext(transactionalSession).opptegnelseRepository,
        )

    internal fun overstyringIgangsatt(
        melding: OverstyringIgangsatt,
        transactionalSession: TransactionalSession,
    ): OverstyringIgangsattCommand =
        OverstyringIgangsattCommand(
            berørteVedtaksperiodeIder = melding.berørteVedtaksperiodeIder,
            kilde = melding.kilde,
            overstyringRepository = OverstyringDao(transactionalSession),
        )

    internal fun utbetalingEndret(
        hendelse: UtbetalingEndret,
        session: TransactionalSession,
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
            utbetalingRepository = UtbetalingDao(session),
            opptegnelseRepository = repositories.withSessionContext(session).opptegnelseRepository,
            reservasjonDao = repositories.withSessionContext(session).reservasjonDao,
            oppgaveDao = repositories.withSessionContext(session).oppgaveDao,
            tildelingDao = repositories.withSessionContext(session).tildelingDao,
            oppgaveService = transaksjonellOppgaveService(session),
            totrinnsvurderingService = lagTotrinnsvurderingService(repositories.withSessionContext(session)),
            json = hendelse.toJson(),
        )

    internal fun vedtaksperiodeForkastet(
        hendelse: VedtaksperiodeForkastet,
        session: TransactionalSession,
    ): VedtaksperiodeForkastetCommand =
        VedtaksperiodeForkastetCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            id = hendelse.id,
            commandContextDao = repositories.withSessionContext(session).commandContextDao,
            oppgaveService = transaksjonellOppgaveService(session),
            reservasjonDao = repositories.withSessionContext(session).reservasjonDao,
            tildelingDao = repositories.withSessionContext(session).tildelingDao,
            oppgaveDao = repositories.withSessionContext(session).oppgaveDao,
            totrinnsvurderingService = lagTotrinnsvurderingService(repositories.withSessionContext(session)),
        )

    internal fun stansAutomatiskBehandling(
        hendelse: StansAutomatiskBehandlingMelding,
        session: Session,
    ) = ikkesuspenderendeCommand {
        StansAutomatiskBehandlingMediator.Factory
            .stansAutomatiskBehandlingMediator(
                repositories.withSessionContext(session),
                subsumsjonsmelderProvider,
            ).håndter(hendelse)
    }

    internal fun håndterAvvikVurdert(
        avviksvurdering: AvviksvurderingDto,
        session: Session,
    ) = ikkesuspenderendeCommand {
        repositories.withSessionContext(session).avviksvurderingDao.lagre(avviksvurdering)
    }

    internal fun løsGodkjenningsbehov(
        melding: Saksbehandlerløsning,
        person: Person,
        transactionalSession: TransactionalSession,
    ): LøsGodkjenningsbehov {
        val godkjenningsbehov = MeldingDao(transactionalSession).finnGodkjenningsbehov(melding.godkjenningsbehovhendelseId)
        val oppgaveId = melding.oppgaveId
        val sykefraværstilfelle = person.sykefraværstilfelle(godkjenningsbehov.vedtaksperiodeId())
        val utbetaling =
            UtbetalingDao(transactionalSession).utbetalingFor(oppgaveId)
                ?: throw IllegalStateException("Forventer å finne utbetaling for oppgave med id=$oppgaveId")
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
        session: TransactionalSession,
    ): GodkjenningsbehovCommand {
        val utbetaling = UtbetalingDao(session).hentUtbetaling(godkjenningsbehovData.utbetalingId)
        val førsteKjenteDagFinner = { generasjonService.førsteKjenteDag(godkjenningsbehovData.fødselsnummer) }
        return GodkjenningsbehovCommand(
            behovData = godkjenningsbehovData,
            utbetaling = utbetaling,
            førsteKjenteDagFinner = førsteKjenteDagFinner,
            automatisering = transaksjonellAutomatisering(session),
            vedtakDao = repositories.withSessionContext(session).vedtakDao,
            commandContextDao = repositories.withSessionContext(session).commandContextDao,
            personRepository = PersonDao(session),
            inntektskilderRepository = repositories.withSessionContext(session).inntektskilderRepository,
            arbeidsforholdDao = repositories.withSessionContext(session).arbeidsforholdDao,
            egenAnsattDao = repositories.withSessionContext(session).egenAnsattDao,
            utbetalingRepository = UtbetalingDao(session),
            vergemålRepository = VergemålDao(session),
            åpneGosysOppgaverDao = repositories.withSessionContext(session).åpneGosysOppgaverDao,
            risikovurderingRepository = RisikovurderingDao(session),
            påVentRepository = PåVentDao(session),
            overstyringRepository = OverstyringDao(session),
            automatiseringDao = repositories.withSessionContext(session).automatiseringDao,
            oppgaveDao = repositories.withSessionContext(session).oppgaveDao,
            avviksvurderingDao = repositories.withSessionContext(session).avviksvurderingDao,
            oppgaveService = transaksjonellOppgaveService(session),
            godkjenningMediator = GodkjenningMediator(repositories.withSessionContext(session).opptegnelseRepository),
            totrinnsvurderingService = lagTotrinnsvurderingService(repositories.withSessionContext(session)),
            person = person,
        )
    }

    // Kanskje prøve å få håndtering av søknad inn i samme flyt som andre kommandokjeder
    internal fun iverksettSøknadSendt(
        melding: SøknadSendt,
        commandContextObservers: CommandContextObserver,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { transactionalSession ->
                val transactionalCommandContextDao = repositories.withSessionContext(transactionalSession).commandContextDao
                iverksett(
                    command = søknadSendt(melding, transactionalSession),
                    meldingId = melding.id,
                    commandContext = nyContext(melding.id, transactionalCommandContextDao),
                    commandContextObservers = setOf(commandContextObservers),
                    commandContextDao = transactionalCommandContextDao,
                    metrikkDao = MetrikkDao(transactionalSession),
                )
            }
        }
    }

    private fun nyContext(
        meldingId: UUID,
        transactionalCommandContextDao: CommandContextDao,
    ) = CommandContext(UUID.randomUUID()).apply {
        opprett(transactionalCommandContextDao, meldingId)
    }

    internal fun lagKommandostarter(
        commandContextObservers: Set<CommandContextObserver>,
        commandContext: CommandContext,
        transactionalSession: TransactionalSession,
    ): Kommandostarter =
        { kommandooppretter ->
            val transactionalCommandContextDao = repositories.withSessionContext(transactionalSession).commandContextDao
            val melding = this
            this@Kommandofabrikk.kommandooppretter()?.let { command ->
                iverksett(
                    command = command,
                    meldingId = melding.id,
                    commandContext = commandContext,
                    commandContextObservers = commandContextObservers,
                    commandContextDao = transactionalCommandContextDao,
                    metrikkDao = MetrikkDao(transactionalSession),
                )
            }
        }

    private fun transaksjonellOppgaveService(transactionalSession: TransactionalSession): OppgaveService =
        oppgaveService.nyOppgaveService(repositories.withSessionContext(transactionalSession))

    private fun transaksjonellAutomatisering(transactionalSession: TransactionalSession): Automatisering =
        Automatisering.Factory.automatisering(
            transactionalSession,
            subsumsjonsmelderProvider,
            stikkprøver,
            repositories.withSessionContext(transactionalSession),
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

    private fun lagTotrinnsvurderingService(sessionContext: SessionContext) =
        TotrinnsvurderingService(
            sessionContext.totrinnsvurderingDao,
            sessionContext.oppgaveDao,
            sessionContext.periodehistorikkDao,
            sessionContext.dialogDao,
        )
}
