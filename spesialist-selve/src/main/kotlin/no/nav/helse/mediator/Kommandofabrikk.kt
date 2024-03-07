package no.nav.helse.mediator

import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.KobleVedtaksperiodeTilOverstyringCommand
import no.nav.helse.modell.kommando.TilbakedateringGodkjentCommand
import no.nav.helse.modell.kommando.UtbetalingsgodkjenningCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.EndretEgenAnsattStatusCommand
import no.nav.helse.modell.person.OppdaterPersonsnapshotCommand
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
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovCommand
import no.nav.helse.modell.vedtaksperiode.OpprettVedtaksperiodeCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndretCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastetCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetalingCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOpprettet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnetCommand
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

internal class Kommandofabrikk(
    dataSource: DataSource,
    private val hendelseDao: HendelseDao = HendelseDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val arbeidsgiverDao: ArbeidsgiverDao = ArbeidsgiverDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val tildelingDao: TildelingDao = TildelingDao(dataSource),
    private val saksbehandlerDao: SaksbehandlerDao = SaksbehandlerDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val risikovurderingDao: RisikovurderingDao = RisikovurderingDao(dataSource),
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource),
    private val snapshotDao: SnapshotDao = SnapshotDao(dataSource),
    private val egenAnsattDao: EgenAnsattDao = EgenAnsattDao(dataSource),
    private val generasjonDao: GenerasjonDao = GenerasjonDao(dataSource),
    private val snapshotClient: SnapshotClient,
    oppgaveMediator: () -> OppgaveMediator,
    private val totrinnsvurderingDao: TotrinnsvurderingDao = TotrinnsvurderingDao(dataSource),
    private val notatDao: NotatDao = NotatDao(dataSource),
    private val notatMediator: NotatMediator = NotatMediator(notatDao),
    private val periodehistorikkDao: PeriodehistorikkDao = PeriodehistorikkDao(dataSource),
    private val påVentDao: PåVentDao = PåVentDao(dataSource),
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator = TotrinnsvurderingMediator(
        totrinnsvurderingDao,
        oppgaveDao,
        periodehistorikkDao,
        notatMediator,
    ),
    private val godkjenningMediator: GodkjenningMediator,
    private val automatisering: Automatisering,
    private val arbeidsforholdDao: ArbeidsforholdDao = ArbeidsforholdDao(dataSource),
    private val utbetalingDao: UtbetalingDao = UtbetalingDao(dataSource),
    private val opptegnelseDao: OpptegnelseDao = OpptegnelseDao(dataSource),
    private val generasjonRepository: GenerasjonRepository = GenerasjonRepository(dataSource),
    private val vergemålDao: VergemålDao = VergemålDao(dataSource),
    private val varselRepository: ActualVarselRepository = ActualVarselRepository(dataSource),
) {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val sykefraværstilfelleDao = SykefraværstilfelleDao(dataSource)
    private val avviksvurderingDao = AvviksvurderingDao(dataSource)
    private val oppgaveMediator: OppgaveMediator by lazy { oppgaveMediator() }

    internal fun sykefraværstilfelle(fødselsnummer: String, skjæringstidspunkt: LocalDate): Sykefraværstilfelle {
        val gjeldendeGenerasjoner = generasjonerFor(fødselsnummer, skjæringstidspunkt)
        val skjønnsfastsatteSykepengegrunnlag = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer, skjæringstidspunkt)
        return Sykefraværstilfelle(fødselsnummer, skjæringstidspunkt, gjeldendeGenerasjoner, skjønnsfastsatteSykepengegrunnlag)
    }

    private fun generasjonerFor(fødselsnummer: String, skjæringstidspunkt: LocalDate): List<Generasjon> {
        return gjeldendeGenerasjoner {
            generasjonRepository.finnVedtaksperiodeIderFor(fødselsnummer, skjæringstidspunkt)
        }
    }

    private fun generasjonerFor(utbetalingId: UUID): List<Generasjon> {
        return gjeldendeGenerasjoner {
            generasjonRepository.finnVedtaksperiodeIderFor(utbetalingId)
        }
    }

    private fun gjeldendeGenerasjoner(iderGetter: () -> Set<UUID>): List<Generasjon> {
        return iderGetter().map {
            gjeldendeGenerasjon(it)
        }
    }

    private fun gjeldendeGenerasjon(vedtaksperiodeId: UUID): Generasjon {
        return GenerasjonBuilder(vedtaksperiodeId = vedtaksperiodeId).build(generasjonRepository, varselRepository)
    }

    private fun førsteGenerasjon(vedtaksperiodeId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate): Generasjon {
        return GenerasjonBuilder(vedtaksperiodeId).buildFirst(
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            observers = arrayOf(generasjonRepository, varselRepository)
        )
    }

    internal fun avviksvurdering(avviksvurdering: AvviksvurderingDto) {
        avviksvurderingDao.lagre(avviksvurdering)
    }

    fun endretEgenAnsattStatus(fødselsnummer: String, hendelse: EndretEgenAnsattStatus): EndretEgenAnsattStatusCommand {
        return EndretEgenAnsattStatusCommand(
            fødselsnummer = fødselsnummer,
            erEgenAnsatt = hendelse.erEgenAnsatt,
            opprettet = hendelse.opprettet,
            egenAnsattDao = egenAnsattDao,
            oppgaveMediator = oppgaveMediator
        )
    }

    fun opprettVedtaksperiode(fødselsnummer: String, hendelse: VedtaksperiodeOpprettet): OpprettVedtaksperiodeCommand {
        return OpprettVedtaksperiodeCommand(
            id = hendelse.id,
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            fødselsnummer = fødselsnummer,
            generasjon = førsteGenerasjon(hendelse.vedtaksperiodeId(), hendelse.fom, hendelse.tom, hendelse.skjæringstidspunkt),
            organisasjonsnummer = hendelse.organisasjonsnummer,
            fom = hendelse.fom,
            tom = hendelse.tom,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao
        )
    }

    fun gosysOppgaveEndret(fødselsnummer: String, hendelse: GosysOppgaveEndret): GosysOppgaveEndretCommand {
        val oppgaveDataForAutomatisering = oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer).let { oppgaveId ->
            oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)!!
        }

        val skjæringstidspunkt = generasjonRepository.skjæringstidspunktFor(oppgaveDataForAutomatisering.vedtaksperiodeId)

        val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)
        val oppgaveId by lazy { oppgaveDao.finnOppgaveId(fødselsnummer) }
        val harTildeltOppgave = oppgaveId?.let { tildelingDao.tildelingForOppgave(it) != null } ?: false

        return GosysOppgaveEndretCommand(
            id = hendelse.id,
            fødselsnummer = fødselsnummer,
            aktørId = requireNotNull(personDao.finnAktørId(fødselsnummer)),
            utbetaling = utbetaling,
            sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, skjæringstidspunkt),
            harTildeltOppgave = harTildeltOppgave,
            oppgavedataForAutomatisering = oppgaveDataForAutomatisering,
            automatisering = automatisering,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            oppgaveDao = oppgaveDao,
            oppgaveMediator = oppgaveMediator,
            godkjenningMediator = godkjenningMediator
        )
    }

    fun tilbakedateringGodkjent(fødselsnummer: String): TilbakedateringGodkjentCommand {
        val oppgaveDataForAutomatisering = oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer).let { oppgaveId ->
            oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)!!
        }
        val sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, oppgaveDataForAutomatisering.skjæringstidspunkt)
        val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)

        sikkerLog.info("Henter oppgaveDataForAutomatisering ifm. godkjent tilbakedatering for fnr $fødselsnummer og vedtaksperiodeId ${oppgaveDataForAutomatisering.vedtaksperiodeId}")

        return TilbakedateringGodkjentCommand(
            fødselsnummer = fødselsnummer,
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
            automatisering = automatisering,
            oppgaveDataForAutomatisering = oppgaveDataForAutomatisering,
            oppgaveMediator = oppgaveMediator,
            godkjenningMediator = godkjenningMediator
        )
    }

    fun vedtaksperiodeReberegnet(hendelse: VedtaksperiodeReberegnet): VedtaksperiodeReberegnetCommand {
        return VedtaksperiodeReberegnetCommand(
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            utbetalingDao = utbetalingDao,
            periodehistorikkDao = periodehistorikkDao,
            commandContextDao = commandContextDao,
            oppgaveMediator = oppgaveMediator
        )
    }

    fun vedtaksperiodeNyUtbetaling(hendelse: VedtaksperiodeNyUtbetaling): VedtaksperiodeNyUtbetalingCommand {
        return VedtaksperiodeNyUtbetalingCommand(
            id = hendelse.id,
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            utbetalingId = hendelse.utbetalingId,
            utbetalingDao = utbetalingDao,
            gjeldendeGenerasjon = gjeldendeGenerasjon(hendelse.vedtaksperiodeId())
        )
    }

    fun søknadSendt(hendelse: SøknadSendt): SøknadSendtCommand {
        return SøknadSendtCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            aktørId = hendelse.aktørId,
            organisasjonsnummer = hendelse.organisasjonsnummer,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao
        )
    }

    fun oppdaterPersonsnapshot(hendelse: Personmelding): OppdaterPersonsnapshotCommand {
        return OppdaterPersonsnapshotCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            førsteKjenteDagFinner = { generasjonRepository.førsteKjenteDag(hendelse.fødselsnummer()) },
            personDao = personDao,
            snapshotDao = snapshotDao,
            opptegnelseDao = opptegnelseDao,
            snapshotClient = snapshotClient
        )
    }

    fun kobleVedtaksperiodeTilOverstyring(hendelse: OverstyringIgangsatt): KobleVedtaksperiodeTilOverstyringCommand {
        return KobleVedtaksperiodeTilOverstyringCommand(
            berørteVedtaksperiodeIder = hendelse.berørteVedtaksperiodeIder,
            kilde = hendelse.kilde,
            overstyringDao = overstyringDao,
        )
    }

    fun utbetalingAnnullert(hendelse: UtbetalingAnnullert): UtbetalingAnnullertCommand {
        return UtbetalingAnnullertCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            utbetalingId = hendelse.utbetalingId,
            saksbehandlerEpost = hendelse.saksbehandlerEpost,
            annullertTidspunkt = hendelse.annullertTidspunkt,
            utbetalingDao = utbetalingDao,
            personDao = personDao,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            saksbehandlerDao = saksbehandlerDao
        )
    }

    fun utbetalingEndret(hendelse: UtbetalingEndret): UtbetalingEndretCommand {
        return UtbetalingEndretCommand(
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
            gjeldendeGenerasjoner = generasjonerFor(hendelse.utbetalingId),
            utbetalingDao = utbetalingDao,
            opptegnelseDao = opptegnelseDao,
            reservasjonDao = reservasjonDao,
            oppgaveDao = oppgaveDao,
            tildelingDao = tildelingDao,
            oppgaveMediator = oppgaveMediator,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
            json = hendelse.toJson()
        )
    }

    fun vedtaksperiodeEndret(hendelse: VedtaksperiodeEndret): VedtaksperiodeEndretCommand {
        return VedtaksperiodeEndretCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            forårsaketAvId = hendelse.forårsaketAvId,
            forrigeTilstand = hendelse.forrigeTilstand,
            gjeldendeTilstand = hendelse.gjeldendeTilstand,
            gjeldendeGenerasjon = gjeldendeGenerasjon(hendelse.vedtaksperiodeId()),
            personDao = personDao,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient
        )
    }

    fun vedtaksperiodeForkastet(hendelse: VedtaksperiodeForkastet): VedtaksperiodeForkastetCommand {
        return VedtaksperiodeForkastetCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            id = hendelse.id,
            personDao = personDao,
            commandContextDao = commandContextDao,
            snapshotDao = snapshotDao,
            vedtakDao = vedtakDao,
            snapshotClient = snapshotClient,
            oppgaveMediator = oppgaveMediator
        )
    }

    fun utbetalingsgodkjenning(hendelse: Saksbehandlerløsning): UtbetalingsgodkjenningCommand {
        val oppgaveId = hendelse.oppgaveId
        val fødselsnummer = hendelse.fødselsnummer()
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
        val skjæringstidspunkt = generasjonRepository.skjæringstidspunktFor(vedtaksperiodeId)
        val sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, skjæringstidspunkt)
        val utbetaling = utbetalingDao.utbetalingFor(oppgaveId)
        return UtbetalingsgodkjenningCommand(
            id = hendelse.id,
            behandlingId = hendelse.behandlingId,
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
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
            hendelseDao = hendelseDao,
            godkjenningMediator = godkjenningMediator
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
            tags = hendelse.tags,
            periodeFom = hendelse.periodeFom,
            periodeTom = hendelse.periodeTom,
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
            generasjonDao = generasjonDao,
            utbetalingDao = utbetalingDao,
            vergemålDao = vergemålDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            risikovurderingDao = risikovurderingDao,
            påVentDao = påVentDao,
            overstyringDao = overstyringDao,
            periodehistorikkDao = periodehistorikkDao,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            oppgaveMediator = oppgaveMediator,
            godkjenningMediator = godkjenningMediator,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
            json = hendelse.toJson()
        )
    }
}
