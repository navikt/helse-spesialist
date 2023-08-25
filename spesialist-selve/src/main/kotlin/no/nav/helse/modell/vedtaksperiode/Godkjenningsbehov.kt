package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.command.KlargjørArbeidsforholdCommand
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringCommand
import no.nav.helse.modell.automatisering.AutomatiskAvvisningCommand
import no.nav.helse.modell.egenansatt.EgenAnsattCommand
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.AvbrytContextCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.KlargjørArbeidsgiverCommand
import no.nav.helse.modell.kommando.KlargjørPersonCommand
import no.nav.helse.modell.kommando.KlargjørVedtaksperiodeCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilHendelseCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandleroppgaveCommand
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.kommando.PersisterPeriodehistorikkCommand
import no.nav.helse.modell.kommando.TrengerTotrinnsvurderingCommand
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikoCommand
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.LeggPåVarslerCommand
import no.nav.helse.modell.vergemal.VergemålCommand
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator

internal class Godkjenningsbehov(
    override val id: UUID,
    private val fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    utbetalingId: UUID,
    periodeFom: LocalDate,
    periodeTom: LocalDate,
    periodetype: Periodetype,
    førstegangsbehandling: Boolean,
    utbetalingtype: Utbetalingtype,
    kanAvvises: Boolean,
    inntektskilde: Inntektskilde,
    orgnummereMedRelevanteArbeidsforhold: List<String>,
    skjæringstidspunkt: LocalDate,
    sykefraværstilfelle: Sykefraværstilfelle,
    private val json: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    commandContextDao: CommandContextDao,
    risikovurderingDao: RisikovurderingDao,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    egenAnsattDao: EgenAnsattDao,
    arbeidsforholdDao: ArbeidsforholdDao,
    vergemålDao: VergemålDao,
    snapshotClient: SnapshotClient,
    oppgaveMediator: OppgaveMediator,
    automatisering: Automatisering,
    godkjenningMediator: GodkjenningMediator,
    utbetalingDao: UtbetalingDao,
    periodehistorikkDao: PeriodehistorikkDao,
    overstyringDao: OverstyringDao,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    snapshotMediator: SnapshotMediator,
) : Hendelse, MacroCommand() {

    // lambda fordi mange av testene ikke sørger for at utbetalingen fins i databasen før godkjenningsbehovet behandles
    private val utbetalingsfinner = { snapshotMediator.finnUtbetaling(fødselsnummer, utbetalingId) }
    private val utbetaling = utbetalingDao.hentUtbetaling(utbetalingId)

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
            personDao = personDao
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
        EgenAnsattCommand(
            egenAnsattDao = egenAnsattDao,
        ),
        VergemålCommand(
            hendelseId = id,
            vergemålDao = vergemålDao,
            vedtaksperiodeId = vedtaksperiodeId,
            sykefraværstilfelle = sykefraværstilfelle
        ),
        ÅpneGosysOppgaverCommand(
            hendelseId = id,
            aktørId = aktørId,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            vedtaksperiodeId = vedtaksperiodeId,
            sykefraværstilfelle = sykefraværstilfelle,
            harTildeltOppgave = false
        ),
        RisikoCommand(
            hendelseId = id,
            vedtaksperiodeId = vedtaksperiodeId,
            risikovurderingDao = risikovurderingDao,
            organisasjonsnummer = organisasjonsnummer,
            førstegangsbehandling = førstegangsbehandling,
            sykefraværstilfelle = sykefraværstilfelle,
            utbetalingsfinner = utbetalingsfinner,
        ),
        AutomatiskAvvisningCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vergemålDao = vergemålDao,
            godkjenningMediator = godkjenningMediator,
            hendelseId = id,
            utbetaling = utbetaling,
            kanAvvises = kanAvvises,
        ),
        AutomatiseringCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = id,
            automatisering = automatisering,
            godkjenningsbehovJson = json,
            godkjenningMediator = godkjenningMediator,
            utbetaling = utbetaling,
            periodetype = periodetype,
            sykefraværstilfelle = sykefraværstilfelle,
            periodeTom = periodeTom
        ),
        OpprettSaksbehandleroppgaveCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            hendelseId = id,
            personDao = personDao,
            risikovurderingDao = risikovurderingDao,
            utbetalingId = utbetalingId,
            utbetalingtype = utbetalingtype,
            snapshotMediator = snapshotMediator,
        ),
        TrengerTotrinnsvurderingCommand(
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
        ),
        LeggPåVarslerCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            personDao = personDao,
            vergemålDao = vergemålDao,
            hendelseId = id,
            utbetaling = utbetaling,
            sykefraværstilfelle = sykefraværstilfelle
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

}
