package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.VedtaksperiodemeldingOld
import no.nav.helse.mediator.oppgave.OppgaveService
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
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersonCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilHendelseCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandleroppgave
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.kommando.PersisterPeriodehistorikkCommand
import no.nav.helse.modell.kommando.PersisterVedtaksperiodetypeCommand
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
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient
import java.time.LocalDate
import java.util.UUID

data class SpleisVedtaksperiode(
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
) {
    internal fun erRelevant(vedtaksperiodeId: UUID): Boolean = this.vedtaksperiodeId == vedtaksperiodeId
}

internal class Godkjenningsbehov private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
    val utbetalingId: UUID,
    val spleisBehandlingId: UUID,
    val tags: List<String>,
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
) : VedtaksperiodemeldingOld {
    override fun fødselsnummer() = fødselsnummer

    override fun vedtaksperiodeId() = vedtaksperiodeId

    override fun toJson() = json

    internal constructor(packet: JsonMessage) : this(
        id = packet["@id"].asUUID(),
        fødselsnummer = packet["fødselsnummer"].asText(),
        aktørId = packet["aktørId"].asText(),
        organisasjonsnummer = packet["organisasjonsnummer"].asText(),
        periodeFom = LocalDate.parse(packet["Godkjenning.periodeFom"].asText()),
        periodeTom = LocalDate.parse(packet["Godkjenning.periodeTom"].asText()),
        skjæringstidspunkt = LocalDate.parse(packet["Godkjenning.skjæringstidspunkt"].asText()),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        spleisVedtaksperioder =
            packet["Godkjenning.perioderMedSammeSkjæringstidspunkt"].map { periodeNode ->
                SpleisVedtaksperiode(
                    vedtaksperiodeId = periodeNode["vedtaksperiodeId"].asUUID(),
                    spleisBehandlingId = periodeNode["behandlingId"].asUUID(),
                    fom = periodeNode["fom"].asLocalDate(),
                    tom = periodeNode["tom"].asLocalDate(),
                    skjæringstidspunkt = packet["Godkjenning.skjæringstidspunkt"].asLocalDate(),
                )
            },
        spleisBehandlingId = UUID.fromString(packet["Godkjenning.behandlingId"].asText()),
        tags = packet["Godkjenning.tags"].takeUnless(JsonNode::isMissingOrNull)?.map { it.asText() }?.toList() ?: emptyList<String>(),
        utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
        periodetype = Periodetype.valueOf(packet["Godkjenning.periodetype"].asText()),
        førstegangsbehandling = packet["Godkjenning.førstegangsbehandling"].asBoolean(),
        utbetalingtype = Utbetalingtype.valueOf(packet["Godkjenning.utbetalingtype"].asText()),
        inntektskilde = Inntektskilde.valueOf(packet["Godkjenning.inntektskilde"].asText()),
        orgnummereMedRelevanteArbeidsforhold =
            packet["Godkjenning.orgnummereMedRelevanteArbeidsforhold"]
                .takeUnless(JsonNode::isMissingOrNull)
                ?.map { it.asText() } ?: emptyList(),
        kanAvvises = packet["Godkjenning.kanAvvises"].asBoolean(),
        json = packet.toJson(),
    )

    internal constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode.path("@id").asText()),
        fødselsnummer = jsonNode.path("fødselsnummer").asText(),
        aktørId = jsonNode.path("aktørId").asText(),
        organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
        periodeFom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeFom").asText()),
        periodeTom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeTom").asText()),
        vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
        spleisVedtaksperioder =
            jsonNode.path("Godkjenning").path("perioderMedSammeSkjæringstidspunkt").map { periodeNode ->
                SpleisVedtaksperiode(
                    vedtaksperiodeId = periodeNode["vedtaksperiodeId"].asUUID(),
                    spleisBehandlingId = periodeNode["behandlingId"].asUUID(),
                    fom = periodeNode["fom"].asLocalDate(),
                    tom = periodeNode["tom"].asLocalDate(),
                    skjæringstidspunkt = jsonNode.path("Godkjenning").path("skjæringstidspunkt").asLocalDate(),
                )
            },
        spleisBehandlingId = UUID.fromString(jsonNode.path("Godkjenning").path("behandlingId").asText()),
        tags =
            jsonNode
                .path("Godkjenning")
                .path("tags")
                .takeUnless(JsonNode::isMissingOrNull)
                ?.map {
                    it.asText()
                }?.toList() ?: emptyList<String>(),
        utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText()),
        skjæringstidspunkt = LocalDate.parse(jsonNode.path("Godkjenning").path("skjæringstidspunkt").asText()),
        periodetype = Periodetype.valueOf(jsonNode.path("Godkjenning").path("periodetype").asText()),
        førstegangsbehandling = jsonNode.path("Godkjenning").path("førstegangsbehandling").asBoolean(),
        utbetalingtype = Utbetalingtype.valueOf(jsonNode.path("Godkjenning").path("utbetalingtype").asText()),
        inntektskilde = Inntektskilde.valueOf(jsonNode.path("Godkjenning").path("inntektskilde").asText()),
        orgnummereMedRelevanteArbeidsforhold =
            jsonNode
                .path("Godkjenning")
                .path("orgnummereMedRelevanteArbeidsforhold")
                .takeUnless(JsonNode::isMissingOrNull)
                ?.map { it.asText() } ?: emptyList(),
        kanAvvises = jsonNode.path("Godkjenning").path("kanAvvises").asBoolean(),
        json = jsonNode.toString(),
    )
}

internal class GodkjenningsbehovCommand(
    id: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    orgnummereMedRelevanteArbeidsforhold: List<String>,
    vedtaksperiodeId: UUID,
    spleisBehandlingId: UUID,
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
    snapshotClient: ISnapshotClient,
    oppgaveService: OppgaveService,
    generasjonRepository: GenerasjonRepository,
    godkjenningMediator: GodkjenningMediator,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    json: String,
    spleisVedtaksperioder: List<SpleisVedtaksperiode>,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OpprettKoblingTilHendelseCommand(
                hendelseId = id,
                vedtaksperiodeId = vedtaksperiodeId,
                vedtakDao = vedtakDao,
            ),
            AvbrytContextCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                commandContextDao = commandContextDao,
            ),
            PersisterVedtaksperiodetypeCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                vedtaksperiodetype = periodetype,
                inntektskilde = inntektskilde,
                vedtakDao = vedtakDao,
            ),
            OpprettKoblingTilUtbetalingCommand(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                utbetalingDao = utbetalingDao,
            ),
            ForberedVisningCommand(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
                førstegangsbehandling = førstegangsbehandling,
                førsteKjenteDagFinner = førsteKjenteDagFinner,
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                arbeidsforholdDao = arbeidsforholdDao,
                snapshotDao = snapshotDao,
                snapshotClient = snapshotClient,
            ),
            KontrollerEgenAnsattstatus(
                fødselsnummer = fødselsnummer,
                egenAnsattDao = egenAnsattDao,
            ),
            VurderVergemålOgFullmakt(
                hendelseId = id,
                vergemålDao = vergemålDao,
                vedtaksperiodeId = vedtaksperiodeId,
                sykefraværstilfelle = sykefraværstilfelle,
            ),
            VurderEnhetUtland(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                personDao = personDao,
                hendelseId = id,
                sykefraværstilfelle = sykefraværstilfelle,
            ),
            VurderÅpenGosysoppgave(
                hendelseId = id,
                aktørId = aktørId,
                åpneGosysOppgaverDao = åpneGosysOppgaverDao,
                generasjonRepository = generasjonRepository,
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
                spleisBehandlingId = spleisBehandlingId,
                personDao = personDao,
                vergemålDao = vergemålDao,
                godkjenningMediator = godkjenningMediator,
                hendelseId = id,
                utbetaling = utbetaling,
                kanAvvises = kanAvvises,
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
                sykefraværstilfelle = sykefraværstilfelle,
                spleisBehandlingId = spleisBehandlingId,
                organisasjonsnummer = organisasjonsnummer,
            ),
            OpprettSaksbehandleroppgave(
                fødselsnummer = fødselsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                oppgaveService = oppgaveService,
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
                oppgaveService = oppgaveService,
                overstyringDao = overstyringDao,
                totrinnsvurderingMediator = totrinnsvurderingMediator,
                sykefraværstilfelle = sykefraværstilfelle,
                spleisVedtaksperioder = spleisVedtaksperioder,
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
                personDao = personDao,
            ),
        )
}

private class ForberedVisningCommand(
    fødselsnummer: String,
    organisasjonsnummer: String,
    orgnummereMedRelevanteArbeidsforhold: List<String>,
    førstegangsbehandling: Boolean,
    førsteKjenteDagFinner: () -> LocalDate,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    arbeidsforholdDao: ArbeidsforholdDao,
    snapshotDao: SnapshotDao,
    snapshotClient: ISnapshotClient,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterPersonCommand(
                fødselsnummer = fødselsnummer,
                førsteKjenteDagFinner = førsteKjenteDagFinner,
                personDao = personDao,
            ),
            KlargjørArbeidsgiverCommand(
                orgnummere = orgnummereMedRelevanteArbeidsforhold + organisasjonsnummer,
                arbeidsgiverDao = arbeidsgiverDao,
            ),
            KlargjørArbeidsforholdCommand(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                arbeidsforholdDao = arbeidsforholdDao,
                førstegangsbehandling = førstegangsbehandling,
            ),
            OppdaterSnapshotCommand(
                snapshotClient = snapshotClient,
                snapshotDao = snapshotDao,
                fødselsnummer = fødselsnummer,
                personDao = personDao,
            ),
        )
}
