package no.nav.helse.mediator

import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndretCommand
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommand
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.OverstyringIgangsattCommand
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.kommando.TilbakedateringGodkjentCommand
import no.nav.helse.modell.kommando.UtbetalingsgodkjenningCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.EndretEgenAnsattStatusCommand
import no.nav.helse.modell.person.OppdaterPersonsnapshotCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.SøknadSendtCommand
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.modell.utbetaling.UtbetalingAnnullertCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.UtbetalingEndretCommand
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
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
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatRepository
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

internal typealias Kommandostarter = Personmelding.(Kommandofabrikk.() -> Command?) -> Unit

internal class Kommandofabrikk(
    dataSource: DataSource,
    private val meldingDao: MeldingDao = MeldingDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val arbeidsgiverDao: ArbeidsgiverDao = ArbeidsgiverDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val tildelingDao: TildelingDao = TildelingDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val risikovurderingDao: RisikovurderingDao = RisikovurderingDao(dataSource),
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource),
    private val snapshotDao: SnapshotDao = SnapshotDao(dataSource),
    private val egenAnsattDao: EgenAnsattDao = EgenAnsattDao(dataSource),
    private val snapshotClient: ISnapshotClient,
    oppgaveService: () -> OppgaveService,
    private val totrinnsvurderingDao: TotrinnsvurderingDao = TotrinnsvurderingDao(dataSource),
    private val notatDao: NotatDao = NotatDao(dataSource),
    private val notatRepository: NotatRepository = NotatRepository(notatDao),
    private val periodehistorikkDao: PeriodehistorikkDao = PeriodehistorikkDao(dataSource),
    private val påVentDao: PåVentDao = PåVentDao(dataSource),
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator =
        TotrinnsvurderingMediator(
            totrinnsvurderingDao,
            oppgaveDao,
            periodehistorikkDao,
            notatRepository,
        ),
    private val godkjenningMediator: GodkjenningMediator,
    private val automatisering: Automatisering,
    private val arbeidsforholdDao: ArbeidsforholdDao = ArbeidsforholdDao(dataSource),
    private val utbetalingDao: UtbetalingDao = UtbetalingDao(dataSource),
    private val opptegnelseDao: OpptegnelseDao = OpptegnelseDao(dataSource),
    private val generasjonRepository: GenerasjonRepository = GenerasjonRepository(dataSource),
    private val vergemålDao: VergemålDao = VergemålDao(dataSource),
) {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val avviksvurderingDao = AvviksvurderingDao(dataSource)
    private val metrikkDao = MetrikkDao(dataSource)
    private val oppgaveService: OppgaveService by lazy { oppgaveService() }

    internal fun avviksvurdering(avviksvurdering: AvviksvurderingDto) {
        avviksvurderingDao.lagre(avviksvurdering)
    }

    internal fun endretEgenAnsattStatus(melding: EndretEgenAnsattStatus): EndretEgenAnsattStatusCommand =
        EndretEgenAnsattStatusCommand(
            fødselsnummer = melding.fødselsnummer(),
            erEgenAnsatt = melding.erEgenAnsatt,
            opprettet = melding.opprettet,
            egenAnsattDao = egenAnsattDao,
            oppgaveService = oppgaveService,
        )

    internal fun gosysOppgaveEndret(
        melding: GosysOppgaveEndret,
        person: Person,
        oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
    ): GosysOppgaveEndretCommand {
        val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)
        val harTildeltOppgave = tildelingDao.tildelingForOppgave(oppgaveDataForAutomatisering.oppgaveId) != null
        val vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId
        val vedtaksperiode =
            checkNotNull(person.vedtaksperiodeOrNull(vedtaksperiodeId)) {
                "Forventer ikke at denne funksjonen kalles når det ikke finnes en vedtaksperiode med vedtaksperiodeId=$vedtaksperiodeId"
            }

        return GosysOppgaveEndretCommand(
            fødselsnummer = melding.fødselsnummer(),
            aktørId = person.aktørId(),
            utbetaling = utbetaling,
            sykefraværstilfelle = person.sykefraværstilfelle(oppgaveDataForAutomatisering.vedtaksperiodeId),
            harTildeltOppgave = harTildeltOppgave,
            oppgavedataForAutomatisering = oppgaveDataForAutomatisering,
            automatisering = automatisering,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            oppgaveDao = oppgaveDao,
            oppgaveService = oppgaveService,
            godkjenningMediator = godkjenningMediator,
            spleisBehandlingId = vedtaksperiode.gjeldendeBehandlingId,
            organisasjonsnummer = vedtaksperiode.organisasjonsnummer(),
        )
    }

    internal fun tilbakedateringGodkjent(
        melding: TilbakedateringBehandlet,
        person: Person,
        oppgaveDataForAutomatisering: OppgaveDataForAutomatisering,
    ): TilbakedateringGodkjentCommand {
        val vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId
        val sykefraværstilfelle = person.sykefraværstilfelle(vedtaksperiodeId)
        val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)
        val vedtaksperiode =
            checkNotNull(person.vedtaksperiodeOrNull(vedtaksperiodeId)) {
                "Forventer ikke at denne funksjonen kalles når det ikke finnes en vedtaksperiode med vedtaksperiodeId=$vedtaksperiodeId"
            }

        return TilbakedateringGodkjentCommand(
            fødselsnummer = melding.fødselsnummer(),
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
            automatisering = automatisering,
            oppgaveDataForAutomatisering = oppgaveDataForAutomatisering,
            oppgaveService = oppgaveService,
            godkjenningMediator = godkjenningMediator,
            spleisBehandlingId = vedtaksperiode.gjeldendeBehandlingId,
            organisasjonsnummer = vedtaksperiode.organisasjonsnummer(),
            søknadsperioder = melding.perioder,
        )
    }

    internal fun finnOppgavedata(fødselsnummer: String): OppgaveDataForAutomatisering? {
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

    internal fun vedtaksperiodeReberegnet(hendelse: VedtaksperiodeReberegnet): VedtaksperiodeReberegnetCommand =
        VedtaksperiodeReberegnetCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            utbetalingDao = utbetalingDao,
            periodehistorikkDao = periodehistorikkDao,
            commandContextDao = commandContextDao,
            oppgaveService = oppgaveService,
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao,
            oppgaveDao = oppgaveDao,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
        )

    internal fun vedtaksperiodeNyUtbetaling(hendelse: VedtaksperiodeNyUtbetaling): VedtaksperiodeNyUtbetalingCommand =
        VedtaksperiodeNyUtbetalingCommand(
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            utbetalingId = hendelse.utbetalingId,
            utbetalingDao = utbetalingDao,
        )

    fun søknadSendt(hendelse: SøknadSendt): SøknadSendtCommand =
        SøknadSendtCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            aktørId = hendelse.aktørId,
            organisasjonsnummer = hendelse.organisasjonsnummer,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
        )

    internal fun adressebeskyttelseEndret(melding: AdressebeskyttelseEndret): AdressebeskyttelseEndretCommand =
        AdressebeskyttelseEndretCommand(melding.fødselsnummer(), personDao, oppgaveDao, godkjenningMediator)

    internal fun oppdaterPersonsnapshot(hendelse: Personmelding): OppdaterPersonsnapshotCommand =
        OppdaterPersonsnapshotCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            førsteKjenteDagFinner = { generasjonRepository.førsteKjenteDag(hendelse.fødselsnummer()) },
            personDao = personDao,
            snapshotDao = snapshotDao,
            opptegnelseDao = opptegnelseDao,
            snapshotClient = snapshotClient,
        )

    internal fun overstyringIgangsatt(melding: OverstyringIgangsatt): OverstyringIgangsattCommand =
        OverstyringIgangsattCommand(
            berørteVedtaksperiodeIder = melding.berørteVedtaksperiodeIder,
            kilde = melding.kilde,
            overstyringDao = overstyringDao,
        )

    internal fun utbetalingAnnullert(hendelse: UtbetalingAnnullert): UtbetalingAnnullertCommand =
        UtbetalingAnnullertCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            personDao = personDao,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
        )

    internal fun utbetalingEndret(hendelse: UtbetalingEndret): UtbetalingEndretCommand =
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
            utbetalingDao = utbetalingDao,
            opptegnelseDao = opptegnelseDao,
            reservasjonDao = reservasjonDao,
            oppgaveDao = oppgaveDao,
            tildelingDao = tildelingDao,
            oppgaveService = oppgaveService,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
            json = hendelse.toJson(),
        )

    internal fun vedtaksperiodeForkastet(hendelse: VedtaksperiodeForkastet): VedtaksperiodeForkastetCommand =
        VedtaksperiodeForkastetCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            id = hendelse.id,
            personDao = personDao,
            commandContextDao = commandContextDao,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            oppgaveService = oppgaveService,
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao,
            oppgaveDao = oppgaveDao,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
        )

    internal fun utbetalingsgodkjenning(
        melding: Saksbehandlerløsning,
        person: Person,
    ): UtbetalingsgodkjenningCommand {
        val oppgaveId = melding.oppgaveId
        val fødselsnummer = melding.fødselsnummer()
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
        val spleisBehandlingId = person.vedtaksperiodeOrNull(vedtaksperiodeId)?.gjeldendeBehandlingId
        val sykefraværstilfelle = person.sykefraværstilfelle(vedtaksperiodeId)
        val utbetaling = utbetalingDao.utbetalingFor(oppgaveId)
        return UtbetalingsgodkjenningCommand(
            behandlingId = melding.behandlingId,
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId,
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
            godkjenningsbehovhendelseId = melding.godkjenningsbehovhendelseId,
            saksbehandler = melding.saksbehandler,
            beslutter = melding.beslutter,
            meldingDao = meldingDao,
            godkjenningMediator = godkjenningMediator,
        )
    }

    internal fun godkjenningsbehov(
        godkjenningsbehovData: GodkjenningsbehovData,
        person: Person,
    ): GodkjenningsbehovCommand {
        val utbetaling = utbetalingDao.hentUtbetaling(godkjenningsbehovData.utbetalingId)
        val førsteKjenteDagFinner = { generasjonRepository.førsteKjenteDag(godkjenningsbehovData.fødselsnummer) }
        return GodkjenningsbehovCommand(
            commandData = godkjenningsbehovData,
            utbetaling = utbetaling,
            førsteKjenteDagFinner = førsteKjenteDagFinner,
            automatisering = automatisering,
            vedtakDao = vedtakDao,
            commandContextDao = commandContextDao,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            arbeidsforholdDao = arbeidsforholdDao,
            egenAnsattDao = egenAnsattDao,
            utbetalingDao = utbetalingDao,
            vergemålDao = vergemålDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            risikovurderingDao = risikovurderingDao,
            påVentDao = påVentDao,
            overstyringDao = overstyringDao,
            periodehistorikkDao = periodehistorikkDao,
            snapshotDao = snapshotDao,
            oppgaveDao = oppgaveDao,
            avviksvurderingDao = avviksvurderingDao,
            snapshotClient = snapshotClient,
            oppgaveService = oppgaveService,
            godkjenningMediator = godkjenningMediator,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
            person = person,
        )
    }

    internal fun oppdaterSnapshotCommand(personmelding: Personmelding): OppdaterSnapshotCommand =
        OppdaterSnapshotCommand(snapshotClient, snapshotDao, personmelding.fødselsnummer(), personDao)

    // Kanskje prøve å få håndtering av søknad inn i samme flyt som andre kommandokjeder
    internal fun iverksettSøknadSendt(
        melding: SøknadSendt,
        commandContextObservers: CommandContextObserver,
    ) {
        iverksett(søknadSendt(melding), melding.id, nyContext(melding.id), setOf(commandContextObservers))
    }

    private fun nyContext(meldingId: UUID) =
        CommandContext(UUID.randomUUID()).apply {
            opprett(commandContextDao, meldingId)
        }

    internal fun lagKommandostarter(
        commandContext: CommandContext,
        commandContextObservers: Set<CommandContextObserver>,
    ): Kommandostarter {
        return { kommandooppretter ->
            val melding = this
            this@Kommandofabrikk.kommandooppretter()?.let { command ->
                iverksett(command, melding.id, commandContext, commandContextObservers)
            }
        }
    }

    private fun iverksett(
        command: Command,
        meldingId: UUID,
        commandContext: CommandContext,
        commandContextObservers: Collection<CommandContextObserver>,
    ) {
        commandContextObservers.forEach { commandContext.nyObserver(it) }
        val contextId = commandContext.id()
        withMDC(
            mapOf("contextId" to contextId.toString()),
        ) {
            try {
                if (commandContext.utfør(commandContextDao, meldingId, command)) {
                    val kjøretid = commandContextDao.tidsbrukForContext(contextId)
                    metrikker(command.name, kjøretid, contextId)
                    logg.info(
                        "Kommando(er) for ${command.name} er utført ferdig. Det tok ca {}ms å kjøre hele kommandokjeden",
                        kjøretid,
                    )
                } else {
                    logg.info("${command.name} er suspendert")
                }
            } catch (err: Exception) {
                command.undo(commandContext)
                throw err
            } finally {
                commandContextObservers.forEach { commandContext.avregistrerObserver(it) }
            }
        }
    }

    private fun metrikker(
        hendelsenavn: String,
        kjøretidMs: Int,
        contextId: UUID,
    ) {
        if (hendelsenavn == GodkjenningsbehovCommand::class.simpleName) {
            val utfall: GodkjenningsbehovUtfall = metrikkDao.finnUtfallForGodkjenningsbehov(contextId)
            registrerTidsbrukForGodkjenningsbehov(utfall, kjøretidMs)
        }
        registrerTidsbrukForHendelse(hendelsenavn, kjøretidMs)
    }
}
