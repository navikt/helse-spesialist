package no.nav.helse.mediator

import no.nav.helse.db.AnnulleringDao
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.builders.GenerasjonBuilder
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
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.modell.person.OppdaterPersonsnapshotCommand
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.SøknadSendtCommand
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.modell.utbetaling.UtbetalingAnnullertCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.UtbetalingEndretCommand
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovCommand
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
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

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
    private val generasjonDao: GenerasjonDao = GenerasjonDao(dataSource),
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
    private val varselRepository: VarselRepository = VarselRepository(dataSource),
    private val annulleringDao: AnnulleringDao = AnnulleringDao(dataSource),
) {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
    }

    private val sykefraværstilfelleDao = SykefraværstilfelleDao(dataSource)
    private val avviksvurderingDao = AvviksvurderingDao(dataSource)
    private val metrikkDao = MetrikkDao(dataSource)
    private val oppgaveService: OppgaveService by lazy { oppgaveService() }
    private var commandContext: CommandContext? = null
    private val observers: MutableList<CommandContextObserver> = mutableListOf()

    internal fun nyObserver(vararg observers: CommandContextObserver) {
        this.observers.addAll(observers)
    }

    internal fun avregistrerObserver(vararg observers: CommandContextObserver) {
        this.observers.removeAll(observers.toSet())
    }

    internal fun settEksisterendeContext(commandContext: CommandContext) {
        this.commandContext = commandContext
    }

    internal fun nullstilleEksisterendeContext() {
        this.commandContext = null
    }

    internal fun sykefraværstilfelle(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): Sykefraværstilfelle {
        val gjeldendeGenerasjoner = generasjonerFor(fødselsnummer, skjæringstidspunkt)
        val skjønnsfastsatteSykepengegrunnlag =
            sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(
                fødselsnummer,
                skjæringstidspunkt,
            )
        return Sykefraværstilfelle(
            fødselsnummer,
            skjæringstidspunkt,
            gjeldendeGenerasjoner,
            skjønnsfastsatteSykepengegrunnlag,
        )
    }

    private fun generasjonerFor(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): List<Generasjon> =
        gjeldendeGenerasjoner {
            generasjonRepository.finnVedtaksperiodeIderFor(fødselsnummer, skjæringstidspunkt)
        }

    private fun gjeldendeGenerasjoner(iderGetter: () -> Set<UUID>): List<Generasjon> =
        iderGetter().map {
            gjeldendeGenerasjon(it)
        }

    private fun gjeldendeGenerasjon(vedtaksperiodeId: UUID): Generasjon =
        GenerasjonBuilder(vedtaksperiodeId = vedtaksperiodeId).build(generasjonRepository, varselRepository)

    internal fun avviksvurdering(avviksvurdering: AvviksvurderingDto) {
        avviksvurderingDao.lagre(avviksvurdering)
    }

    private fun endretEgenAnsattStatus(melding: EndretEgenAnsattStatus): EndretEgenAnsattStatusCommand =
        EndretEgenAnsattStatusCommand(
            fødselsnummer = melding.fødselsnummer(),
            erEgenAnsatt = melding.erEgenAnsatt,
            opprettet = melding.opprettet,
            egenAnsattDao = egenAnsattDao,
            oppgaveService = oppgaveService,
        )

    fun gosysOppgaveEndret(
        fødselsnummer: String,
        hendelse: GosysOppgaveEndret,
        person: Person,
    ): GosysOppgaveEndretCommand {
        val oppgaveDataForAutomatisering = hendelse.oppgavedataForAutomatisering

        val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)
        val harTildeltOppgave = tildelingDao.tildelingForOppgave(oppgaveDataForAutomatisering.oppgaveId) != null
        val vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId
        val vedtaksperiode =
            checkNotNull(person.vedtaksperiode(vedtaksperiodeId)) {
                "Forventer ikke at denne funksjonen kalles når det ikke finnes en vedtaksperiode med vedtaksperiodeId=$vedtaksperiodeId"
            }

        return GosysOppgaveEndretCommand(
            id = hendelse.id,
            fødselsnummer = fødselsnummer,
            aktørId = person.aktørId(),
            utbetaling = utbetaling,
            sykefraværstilfelle = person.sykefraværstilfelle(oppgaveDataForAutomatisering.vedtaksperiodeId),
            harTildeltOppgave = harTildeltOppgave,
            oppgavedataForAutomatisering = oppgaveDataForAutomatisering,
            automatisering = automatisering,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            oppgaveDao = oppgaveDao,
            oppgaveService = oppgaveService,
            generasjonRepository = generasjonRepository,
            godkjenningMediator = godkjenningMediator,
            spleisBehandlingId = vedtaksperiode.gjeldendeBehandlingId,
            organisasjonsnummer = vedtaksperiode.organisasjonsnummer(),
        )
    }

    fun tilbakedateringGodkjent(
        fødselsnummer: String,
        melding: TilbakedateringBehandlet,
        person: Person,
    ): TilbakedateringGodkjentCommand {
        val oppgaveDataForAutomatisering = melding.oppgavedataForAutomatisering
        val vedtaksperiodeId = oppgaveDataForAutomatisering.vedtaksperiodeId
        val sykefraværstilfelle = person.sykefraværstilfelle(vedtaksperiodeId)
        val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)
        val vedtaksperiode =
            checkNotNull(person.vedtaksperiode(vedtaksperiodeId)) {
                "Forventer ikke at denne funksjonen kalles når det ikke finnes en vedtaksperiode med vedtaksperiodeId=$vedtaksperiodeId"
            }

        return TilbakedateringGodkjentCommand(
            fødselsnummer = fødselsnummer,
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
            automatisering = automatisering,
            oppgaveDataForAutomatisering = oppgaveDataForAutomatisering,
            oppgaveService = oppgaveService,
            godkjenningMediator = godkjenningMediator,
            spleisBehandlingId = vedtaksperiode.gjeldendeBehandlingId,
            organisasjonsnummer = vedtaksperiode.organisasjonsnummer(),
        )
    }

    private fun vedtaksperiodeReberegnet(hendelse: VedtaksperiodeReberegnet): VedtaksperiodeReberegnetCommand =
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

    private fun vedtaksperiodeNyUtbetaling(hendelse: VedtaksperiodeNyUtbetaling): VedtaksperiodeNyUtbetalingCommand =
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

    private fun adressebeskyttelseEndret(melding: AdressebeskyttelseEndret): AdressebeskyttelseEndretCommand =
        AdressebeskyttelseEndretCommand(melding.fødselsnummer(), personDao, oppgaveDao, godkjenningMediator)

    private fun oppdaterPersonsnapshot(hendelse: Personmelding): OppdaterPersonsnapshotCommand =
        OppdaterPersonsnapshotCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            førsteKjenteDagFinner = { generasjonRepository.førsteKjenteDag(hendelse.fødselsnummer()) },
            personDao = personDao,
            snapshotDao = snapshotDao,
            opptegnelseDao = opptegnelseDao,
            snapshotClient = snapshotClient,
        )

    private fun overstyringIgangsatt(melding: OverstyringIgangsatt): OverstyringIgangsattCommand =
        OverstyringIgangsattCommand(
            berørteVedtaksperiodeIder = melding.berørteVedtaksperiodeIder,
            kilde = melding.kilde,
            overstyringDao = overstyringDao,
        )

    private fun utbetalingAnnullert(hendelse: UtbetalingAnnullert): UtbetalingAnnullertCommand =
        UtbetalingAnnullertCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            utbetalingId = hendelse.utbetalingId,
            utbetalingDao = utbetalingDao,
            personDao = personDao,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            annulleringDao = annulleringDao,
        )

    private fun utbetalingEndret(hendelse: UtbetalingEndret): UtbetalingEndretCommand =
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

    private fun vedtaksperiodeForkastet(hendelse: VedtaksperiodeForkastet): VedtaksperiodeForkastetCommand =
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

    fun utbetalingsgodkjenning(hendelse: Saksbehandlerløsning): UtbetalingsgodkjenningCommand {
        val oppgaveId = hendelse.oppgaveId
        val fødselsnummer = hendelse.fødselsnummer()
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
        val spleisBehandlingId = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)?.spleisBehandlingId
        val skjæringstidspunkt = generasjonRepository.skjæringstidspunktFor(vedtaksperiodeId)
        val sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, skjæringstidspunkt)
        val utbetaling = utbetalingDao.utbetalingFor(oppgaveId)
        return UtbetalingsgodkjenningCommand(
            id = hendelse.id,
            behandlingId = hendelse.behandlingId,
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = spleisBehandlingId,
            utbetaling = utbetaling,
            sykefraværstilfelle = sykefraværstilfelle,
            godkjent = hendelse.godkjent,
            godkjenttidspunkt = hendelse.godkjenttidspunkt,
            ident = hendelse.ident,
            epostadresse = hendelse.epostadresse,
            årsak = hendelse.årsak,
            begrunnelser = hendelse.begrunnelser,
            kommentar = hendelse.kommentar,
            saksbehandleroverstyringer = hendelse.saksbehandleroverstyringer,
            godkjenningsbehovhendelseId = hendelse.godkjenningsbehovhendelseId,
            saksbehandler = hendelse.saksbehandler,
            beslutter = hendelse.beslutter,
            meldingDao = meldingDao,
            godkjenningMediator = godkjenningMediator,
        )
    }

    fun godkjenningsbehov(hendelse: Godkjenningsbehov): GodkjenningsbehovCommand {
        val utbetaling = utbetalingDao.hentUtbetaling(hendelse.utbetalingId)
        val førsteKjenteDagFinner = { generasjonRepository.førsteKjenteDag(hendelse.fødselsnummer()) }
        return GodkjenningsbehovCommand(
            id = hendelse.id,
            fødselsnummer = hendelse.fødselsnummer(),
            aktørId = hendelse.aktørId,
            organisasjonsnummer = hendelse.organisasjonsnummer,
            orgnummereMedRelevanteArbeidsforhold = hendelse.orgnummereMedRelevanteArbeidsforhold,
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            spleisBehandlingId = hendelse.spleisBehandlingId,
            spleisVedtaksperioder = hendelse.spleisVedtaksperioder,
            periodetype = hendelse.periodetype,
            inntektskilde = hendelse.inntektskilde,
            førstegangsbehandling = hendelse.førstegangsbehandling,
            utbetalingId = hendelse.utbetalingId,
            utbetaling = utbetaling,
            utbetalingtype = hendelse.utbetalingtype,
            sykefraværstilfelle = sykefraværstilfelle(hendelse.fødselsnummer(), hendelse.skjæringstidspunkt),
            skjæringstidspunkt = hendelse.skjæringstidspunkt,
            kanAvvises = hendelse.kanAvvises,
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
            snapshotClient = snapshotClient,
            oppgaveService = oppgaveService,
            generasjonRepository = generasjonRepository,
            godkjenningMediator = godkjenningMediator,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
            json = hendelse.toJson(),
        )
    }

    private fun oppdaterSnapshotCommand(personmelding: Personmelding): OppdaterSnapshotCommand =
        OppdaterSnapshotCommand(snapshotClient, snapshotDao, personmelding.fødselsnummer(), personDao)

    internal fun iverksettOppdaterPersonsnapshot(melding: OppdaterPersonsnapshot) {
        iverksett(oppdaterPersonsnapshot(melding), melding.id)
    }

    internal fun iverksettOppdaterSnapshot(melding: Personmelding) {
        iverksett(oppdaterSnapshotCommand(melding), melding.id)
    }

    internal fun iverksettVedtaksperiodeForkastet(melding: VedtaksperiodeForkastet) {
        iverksett(vedtaksperiodeForkastet(melding), melding.id)
    }

    internal fun iverksettUtbetalingEndret(melding: UtbetalingEndret) {
        iverksett(utbetalingEndret(melding), melding.id)
    }

    internal fun iverksettUtbetalingAnnulert(melding: UtbetalingAnnullert) {
        iverksett(utbetalingAnnullert(melding), melding.id)
    }

    internal fun iverksettVedtaksperiodeNyUtbetaling(melding: VedtaksperiodeNyUtbetaling) {
        iverksett(vedtaksperiodeNyUtbetaling(melding), melding.id)
    }

    internal fun iverksettOverstyringIgangsatt(melding: OverstyringIgangsatt) {
        iverksett(overstyringIgangsatt(melding), melding.id)
    }

    internal fun iverksettVedtaksperiodeReberegnet(melding: VedtaksperiodeReberegnet) {
        iverksett(vedtaksperiodeReberegnet(melding), melding.id)
    }

    internal fun iverksettEndretAnsattStatus(melding: EndretEgenAnsattStatus) {
        iverksett(endretEgenAnsattStatus(melding), melding.id)
    }

    internal fun iverksettAdressebeskyttelseEndret(melding: AdressebeskyttelseEndret) {
        iverksett(adressebeskyttelseEndret(melding), melding.id)
    }

    private fun nyContext(meldingId: UUID) =
        CommandContext(UUID.randomUUID()).apply {
            opprett(commandContextDao, meldingId)
        }

    private fun iverksett(
        command: Command,
        meldingId: UUID,
    ) {
        val commandContext = this.commandContext ?: nyContext(meldingId)
        observers.forEach { commandContext.nyObserver(it) }
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
                observers.forEach { commandContext.avregistrerObserver(it) }
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
