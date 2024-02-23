package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Vedtaksperiodehendelse
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.command.KlargjørArbeidsforholdCommand
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.VurderAutomatiskAvvisning
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.gosysoppgaver.VurderÅpenGosysoppgave
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.AvbrytContextCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.KlargjørArbeidsgiverCommand
import no.nav.helse.modell.kommando.KlargjørPersonCommand
import no.nav.helse.modell.kommando.KlargjørVedtaksperiodeCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilHendelseCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandleroppgave
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.kommando.PersisterPeriodehistorikkCommand
import no.nav.helse.modell.kommando.VurderBehovForTotrinnskontroll
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.risiko.VurderVurderingsmomenter
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.VurderEnhetUtland
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.modell.vergemal.VurderVergemålOgFullmakt
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient

internal class Godkjenningsbehov(
    override val id: UUID,
    private val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val periodetype: Periodetype,
    val førstegangsbehandling: Boolean,
    val utbetalingtype: Utbetalingtype,
    val kanAvvises: Boolean,
    val inntektskilde: Inntektskilde,
    val orgnummereMedRelevanteArbeidsforhold: List<String>,
    val skjæringstidspunkt: LocalDate,
    private val json: String,
) : Vedtaksperiodehendelse {

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json
}

internal class GodkjenningsbehovCommand(
    id: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    orgnummereMedRelevanteArbeidsforhold: List<String>,
    vedtaksperiodeId: UUID,
    periodeFom: LocalDate,
    periodeTom: LocalDate,
    periodetype: Periodetype,
    inntektskilde: Inntektskilde,
    førstegangsbehandling: Boolean,
    utbetalingId: UUID,
    utbetaling: Utbetaling,
    utbetalingtype: Utbetalingtype,
    sykefraværstilfelle: Sykefraværstilfelle,
    skjæringstidspunkt: LocalDate,
    kanAvvises: Boolean,
    førsteKjenteDagFinner: () -> LocalDate,
    automatisering: Automatisering,
    vedtakDao: VedtakDao,
    commandContextDao: CommandContextDao,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    arbeidsforholdDao: ArbeidsforholdDao,
    egenAnsattDao: EgenAnsattDao,
    utbetalingDao: UtbetalingDao,
    vergemålDao: VergemålDao,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    risikovurderingDao: RisikovurderingDao,
    påVentDao: PåVentDao,
    overstyringDao: OverstyringDao,
    periodehistorikkDao: PeriodehistorikkDao,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient,
    oppgaveMediator: OppgaveMediator,
    godkjenningMediator: GodkjenningMediator,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    json: String
): MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettKoblingTilHendelseCommand(
            hendelseId = id,
            vedtaksperiodeId = vedtaksperiodeId,
            vedtakDao = vedtakDao
        ),
        AvbrytContextCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            commandContextDao = commandContextDao
        ),
        KlargjørPersonCommand(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            førsteKjenteDagFinner = førsteKjenteDagFinner,
            personDao = personDao,
        ),
        KlargjørArbeidsgiverCommand(
            orgnummere = orgnummereMedRelevanteArbeidsforhold + organisasjonsnummer,
            arbeidsgiverDao = arbeidsgiverDao
        ),
        KlargjørArbeidsforholdCommand(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            arbeidsforholdDao = arbeidsforholdDao,
            førstegangsbehandling = førstegangsbehandling
        ),
        KlargjørVedtaksperiodeCommand(
            snapshotClient = snapshotClient,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            vedtaksperiodetype = periodetype,
            inntektskilde = inntektskilde,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            snapshotDao = snapshotDao,
            vedtakDao = vedtakDao,
            utbetalingId = utbetalingId,
            utbetalingDao = utbetalingDao,
        ),
        KontrollerEgenAnsattstatus(
            fødselsnummer = fødselsnummer,
            egenAnsattDao = egenAnsattDao,
        ),
        VurderVergemålOgFullmakt(
            hendelseId = id,
            vergemålDao = vergemålDao,
            vedtaksperiodeId = vedtaksperiodeId,
            sykefraværstilfelle = sykefraværstilfelle
        ),
        VurderEnhetUtland(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            personDao = personDao,
            hendelseId = id,
            sykefraværstilfelle = sykefraværstilfelle
        ),
        VurderÅpenGosysoppgave(
            hendelseId = id,
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            oppgaveMediator = oppgaveMediator,
            vedtaksperiodeId = vedtaksperiodeId,
            sykefraværstilfelle = sykefraværstilfelle,
            harTildeltOppgave = false,
        ),
        VurderVurderingsmomenter(
            hendelseId = id,
            vedtaksperiodeId = vedtaksperiodeId,
            risikovurderingDao = risikovurderingDao,
            organisasjonsnummer = organisasjonsnummer,
            førstegangsbehandling = førstegangsbehandling,
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
        ),
        VurderAutomatiskAvvisning(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            personDao = personDao,
            vergemålDao = vergemålDao,
            godkjenningMediator = godkjenningMediator,
            hendelseId = id,
            utbetaling = utbetaling,
            kanAvvises = kanAvvises,
            sykefraværstilfelle = sykefraværstilfelle,
        ),
        VurderAutomatiskInnvilgelse(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = id,
            automatisering = automatisering,
            godkjenningsbehovJson = json,
            godkjenningMediator = godkjenningMediator,
            utbetaling = utbetaling,
            periodetype = periodetype,
            sykefraværstilfelle = sykefraværstilfelle
        ),
        OpprettSaksbehandleroppgave(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            hendelseId = id,
            personDao = personDao,
            risikovurderingDao = risikovurderingDao,
            egenAnsattDao = egenAnsattDao,
            utbetalingId = utbetalingId,
            utbetalingtype = utbetalingtype,
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
            vergemålDao = vergemålDao,
            inntektskilde = inntektskilde,
            periodetype = periodetype,
            kanAvvises = kanAvvises,
            vedtakDao = vedtakDao,
            påVentDao = påVentDao,
        ),
        VurderBehovForTotrinnskontroll(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveMediator = oppgaveMediator,
            overstyringDao = overstyringDao,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
            sykefraværstilfelle = sykefraværstilfelle
        ),
        PersisterPeriodehistorikkCommand(
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            periodehistorikkDao = periodehistorikkDao,
            utbetalingDao = utbetalingDao,
        ),
        PersisterInntektCommand(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            personDao = personDao
        )
    )
}
