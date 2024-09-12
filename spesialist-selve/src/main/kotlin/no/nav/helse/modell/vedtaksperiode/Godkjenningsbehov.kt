package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveDao
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
import no.nav.helse.modell.kommando.ForberedBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.kommando.KlargjørArbeidsgiverCommand
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersonCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilAvviksvurdering
import no.nav.helse.modell.kommando.OpprettKoblingTilHendelseCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandleroppgave
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.kommando.PersisterPeriodehistorikkCommand
import no.nav.helse.modell.kommando.PersisterVedtaksperiodetypeCommand
import no.nav.helse.modell.kommando.VurderBehovForTotrinnskontroll
import no.nav.helse.modell.kommando.VurderVidereBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.risiko.VurderVurderingsmomenter
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
    private val spleisVedtaksperioder: List<SpleisVedtaksperiode>,
    private val utbetalingId: UUID,
    private val spleisBehandlingId: UUID,
    private val avviksvurderingId: UUID?,
    private val vilkårsgrunnlagId: UUID,
    private val tags: List<String>,
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
) : Vedtaksperiodemelding {
    override fun fødselsnummer() = fødselsnummer

    override fun vedtaksperiodeId() = vedtaksperiodeId

    override fun toJson() = json

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
    ) {
        kommandostarter { godkjenningsbehov(data(), person) }
    }

    internal fun data(): GodkjenningsbehovData =
        GodkjenningsbehovData(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisVedtaksperioder = spleisVedtaksperioder,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            avviksvurderingId = avviksvurderingId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            tags = tags,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            periodetype = periodetype,
            førstegangsbehandling = førstegangsbehandling,
            utbetalingtype = utbetalingtype,
            kanAvvises = kanAvvises,
            inntektskilde = inntektskilde,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
            skjæringstidspunkt = skjæringstidspunkt,
            json = json,
        )

    internal constructor(packet: JsonMessage) : this(
        id = packet["@id"].asUUID(),
        fødselsnummer = packet["fødselsnummer"].asText(),
        aktørId = packet["aktørId"].asText(),
        organisasjonsnummer = packet["organisasjonsnummer"].asText(),
        periodeFom = LocalDate.parse(packet["Godkjenning.periodeFom"].asText()),
        periodeTom = LocalDate.parse(packet["Godkjenning.periodeTom"].asText()),
        skjæringstidspunkt = LocalDate.parse(packet["Godkjenning.skjæringstidspunkt"].asText()),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        avviksvurderingId =
            packet["avviksvurderingId"].takeUnless { it.isMissingOrNull() }
                ?.let { UUID.fromString(it.asText()) },
        vilkårsgrunnlagId = UUID.fromString(packet["Godkjenning.vilkårsgrunnlagId"].asText()),
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
        tags = packet["Godkjenning.tags"].map { it.asText() },
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

    internal constructor(jsonNode: JsonNode, behandlingIdFinner: (vedtaksperiodeId: UUID) -> UUID) : this(
        id = UUID.fromString(jsonNode.path("@id").asText()),
        fødselsnummer = jsonNode.path("fødselsnummer").asText(),
        aktørId = jsonNode.path("aktørId").asText(),
        organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
        periodeFom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeFom").asText()),
        periodeTom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeTom").asText()),
        vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
        avviksvurderingId =
            jsonNode.path("avviksvurderingId").takeUnless { it.isMissingOrNull() }
                ?.let { UUID.fromString(it.asText()) },
        vilkårsgrunnlagId = UUID.fromString(jsonNode.path("Godkjenning").path("vilkårsgrunnlagId").asText()),
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
        spleisBehandlingId =
            jsonNode.path("Godkjenning")?.path("behandlingId")
                ?.takeUnless { it.isMissingOrNull() }
                ?.let { UUID.fromString(it.asText()) }
                ?: behandlingIdFinner(UUID.fromString(jsonNode.path("vedtaksperiodeId").asText())),
        tags = jsonNode.path("Godkjenning").path("tags").map { it.asText() },
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
    behovData: GodkjenningsbehovData,
    utbetaling: Utbetaling,
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
    oppgaveDao: OppgaveDao,
    avviksvurderingDao: AvviksvurderingDao,
    snapshotClient: ISnapshotClient,
    oppgaveService: OppgaveService,
    godkjenningMediator: GodkjenningMediator,
    totrinnsvurderingMediator: TotrinnsvurderingMediator,
    person: Person,
) : MacroCommand() {
    private val sykefraværstilfelle = person.sykefraværstilfelle(behovData.vedtaksperiodeId)
    override val commands: List<Command> =
        listOf(
            ForberedBehandlingAvGodkjenningsbehov(
                commandData = behovData,
                person = person,
            ),
            OpprettKoblingTilAvviksvurdering(
                commandData = behovData,
                avviksvurderingDao = avviksvurderingDao,
            ),
            VurderVidereBehandlingAvGodkjenningsbehov(
                commandData = behovData,
                utbetalingDao = utbetalingDao,
                oppgaveDao = oppgaveDao,
                vedtakDao = vedtakDao,
            ),
            OpprettKoblingTilHendelseCommand(
                commandData = behovData,
                vedtakDao = vedtakDao,
            ),
            AvbrytContextCommand(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                commandContextDao = commandContextDao,
            ),
            PersisterVedtaksperiodetypeCommand(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                vedtaksperiodetype = behovData.periodetype,
                inntektskilde = behovData.inntektskilde,
                vedtakDao = vedtakDao,
            ),
            OpprettKoblingTilUtbetalingCommand(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                utbetalingId = behovData.utbetalingId,
                utbetalingDao = utbetalingDao,
            ),
            ForberedVisningCommand(
                fødselsnummer = behovData.fødselsnummer,
                organisasjonsnummer = behovData.organisasjonsnummer,
                orgnummereMedRelevanteArbeidsforhold = behovData.orgnummereMedRelevanteArbeidsforhold,
                førsteKjenteDagFinner = førsteKjenteDagFinner,
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                arbeidsforholdDao = arbeidsforholdDao,
                snapshotDao = snapshotDao,
                snapshotClient = snapshotClient,
            ),
            KontrollerEgenAnsattstatus(
                fødselsnummer = behovData.fødselsnummer,
                egenAnsattDao = egenAnsattDao,
            ),
            VurderVergemålOgFullmakt(
                fødselsnummer = behovData.fødselsnummer,
                vergemålDao = vergemålDao,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                sykefraværstilfelle = sykefraværstilfelle,
            ),
            VurderEnhetUtland(
                fødselsnummer = behovData.fødselsnummer,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                personDao = personDao,
                sykefraværstilfelle = sykefraværstilfelle,
            ),
            VurderÅpenGosysoppgave(
                aktørId = behovData.aktørId,
                åpneGosysOppgaverDao = åpneGosysOppgaverDao,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                sykefraværstilfelle = sykefraværstilfelle,
                harTildeltOppgave = false,
                oppgaveService = oppgaveService,
            ),
            VurderVurderingsmomenter(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                risikovurderingDao = risikovurderingDao,
                organisasjonsnummer = behovData.organisasjonsnummer,
                førstegangsbehandling = behovData.førstegangsbehandling,
                sykefraværstilfelle = sykefraværstilfelle,
                utbetaling = utbetaling,
            ),
            VurderAutomatiskAvvisning(
                personDao = personDao,
                vergemålDao = vergemålDao,
                godkjenningMediator = godkjenningMediator,
                utbetaling = utbetaling,
                godkjenningsbehov = behovData,
            ),
            VurderAutomatiskInnvilgelse(
                automatisering = automatisering,
                godkjenningMediator = godkjenningMediator,
                utbetaling = utbetaling,
                sykefraværstilfelle = sykefraværstilfelle,
                godkjenningsbehov = behovData,
            ),
            OpprettSaksbehandleroppgave(
                fødselsnummer = behovData.fødselsnummer,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                oppgaveService = oppgaveService,
                automatisering = automatisering,
                hendelseId = behovData.id,
                personDao = personDao,
                risikovurderingDao = risikovurderingDao,
                egenAnsattDao = egenAnsattDao,
                utbetalingId = behovData.utbetalingId,
                utbetalingtype = behovData.utbetalingtype,
                sykefraværstilfelle = sykefraværstilfelle,
                utbetaling = utbetaling,
                vergemålDao = vergemålDao,
                inntektskilde = behovData.inntektskilde,
                periodetype = behovData.periodetype,
                kanAvvises = behovData.kanAvvises,
                vedtakDao = vedtakDao,
                påVentDao = påVentDao,
            ),
            VurderBehovForTotrinnskontroll(
                fødselsnummer = behovData.fødselsnummer,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                oppgaveService = oppgaveService,
                overstyringDao = overstyringDao,
                totrinnsvurderingMediator = totrinnsvurderingMediator,
                sykefraværstilfelle = sykefraværstilfelle,
                spleisVedtaksperioder = behovData.spleisVedtaksperioder,
            ),
            PersisterPeriodehistorikkCommand(
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                utbetalingId = behovData.utbetalingId,
                periodehistorikkDao = periodehistorikkDao,
                utbetalingDao = utbetalingDao,
            ),
            PersisterInntektCommand(
                fødselsnummer = behovData.fødselsnummer,
                skjæringstidspunkt = behovData.skjæringstidspunkt,
                personDao = personDao,
            ),
        )
}

private class ForberedVisningCommand(
    fødselsnummer: String,
    organisasjonsnummer: String,
    orgnummereMedRelevanteArbeidsforhold: List<String>,
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
            ),
            OppdaterSnapshotCommand(
                snapshotClient = snapshotClient,
                snapshotDao = snapshotDao,
                fødselsnummer = fødselsnummer,
                personDao = personDao,
            ),
        )
}
