package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.db.AutomatiseringDao
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.EgenAnsattDao
import no.nav.helse.db.InntektskilderRepository
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.PåVentDao
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.UtbetalingDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.InntektskildeDto.Companion.gjenopprett
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.VurderAutomatiskAvvisning
import no.nav.helse.modell.automatisering.VurderAutomatiskInnvilgelse
import no.nav.helse.modell.egenansatt.KontrollerEgenAnsattstatus
import no.nav.helse.modell.gosysoppgaver.VurderÅpenGosysoppgave
import no.nav.helse.modell.kommando.AvbrytContextCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.ForberedBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterPersonCommand
import no.nav.helse.modell.kommando.OpprettEllerOppdaterArbeidsforhold
import no.nav.helse.modell.kommando.OpprettEllerOppdaterInntektskilder
import no.nav.helse.modell.kommando.OpprettKoblingTilAvviksvurdering
import no.nav.helse.modell.kommando.OpprettKoblingTilHendelseCommand
import no.nav.helse.modell.kommando.OpprettKoblingTilUtbetalingCommand
import no.nav.helse.modell.kommando.OpprettSaksbehandleroppgave
import no.nav.helse.modell.kommando.PersisterInntektCommand
import no.nav.helse.modell.kommando.PersisterVedtaksperiodetypeCommand
import no.nav.helse.modell.kommando.VurderBehovForTotrinnskontroll
import no.nav.helse.modell.kommando.VurderVidereBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.risiko.VurderVurderingsmomenter
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.VurderEnhetUtland
import no.nav.helse.modell.vergemal.VurderVergemålOgFullmakt
import java.time.LocalDate
import java.util.UUID

data class SpleisSykepengegrunnlagsfakta(
    val arbeidsgivere: List<SykepengegrunnlagsArbeidsgiver>,
)

data class SykepengegrunnlagsArbeidsgiver(
    val arbeidsgiver: String,
    val omregnetÅrsinntekt: Double,
    val inntektskilde: Inntektsopplysningkilde,
    val skjønnsfastsatt: Double?,
)

enum class Inntektsopplysningkilde {
    Arbeidsgiver,
    AOrdningen,
    Saksbehandler,
}

class Godkjenningsbehov(
    override val id: UUID,
    private val fødselsnummer: String,
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
    val spleisSykepengegrunnlagsfakta: SpleisSykepengegrunnlagsfakta,
    private val json: String,
) : Vedtaksperiodemelding {
    override fun fødselsnummer() = fødselsnummer

    override fun vedtaksperiodeId() = vedtaksperiodeId

    override fun toJson() = json

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
    ) {
        kommandostarter { godkjenningsbehov(data(), person, sessionContext) }
    }

    internal fun data(): GodkjenningsbehovData =
        GodkjenningsbehovData(
            id = id,
            fødselsnummer = fødselsnummer,
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
            spleisSykepengegrunnlagsfakta = spleisSykepengegrunnlagsfakta,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
            skjæringstidspunkt = skjæringstidspunkt,
            json = json,
        )

    constructor(jsonNode: JsonNode) : this(
        id = UUID.fromString(jsonNode.path("@id").asText()),
        fødselsnummer = jsonNode.path("fødselsnummer").asText(),
        organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
        periodeFom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeFom").asText()),
        periodeTom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeTom").asText()),
        vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
        avviksvurderingId =
            jsonNode.path("avviksvurderingId").takeUnless { it.isMissingNode || it.isNull }
                ?.let { UUID.fromString(it.asText()) },
        vilkårsgrunnlagId = UUID.fromString(jsonNode.path("Godkjenning").path("vilkårsgrunnlagId").asText()),
        spleisVedtaksperioder =
            jsonNode.path("Godkjenning").path("perioderMedSammeSkjæringstidspunkt").map { periodeNode ->
                SpleisVedtaksperiode(
                    vedtaksperiodeId = periodeNode["vedtaksperiodeId"].asUUID(),
                    spleisBehandlingId = periodeNode["behandlingId"].asUUID(),
                    fom = periodeNode["fom"].asText().let(LocalDate::parse),
                    tom = periodeNode["tom"].asText().let(LocalDate::parse),
                    skjæringstidspunkt = jsonNode.path("Godkjenning").path("skjæringstidspunkt").asText().let(LocalDate::parse),
                )
            },
        spleisBehandlingId = UUID.fromString(jsonNode.path("Godkjenning").path("behandlingId").asText()),
        tags = jsonNode.path("Godkjenning").path("tags").map { it.asText() },
        utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText()),
        skjæringstidspunkt = LocalDate.parse(jsonNode.path("Godkjenning").path("skjæringstidspunkt").asText()),
        periodetype = Periodetype.valueOf(jsonNode.path("Godkjenning").path("periodetype").asText()),
        førstegangsbehandling = jsonNode.path("Godkjenning").path("førstegangsbehandling").asBoolean(),
        utbetalingtype = Utbetalingtype.valueOf(jsonNode.path("Godkjenning").path("utbetalingtype").asText()),
        inntektskilde = Inntektskilde.valueOf(jsonNode.path("Godkjenning").path("inntektskilde").asText()),
        spleisSykepengegrunnlagsfakta =
            SpleisSykepengegrunnlagsfakta(
                arbeidsgivere =
                    jsonNode.path("Godkjenning").get("sykepengegrunnlagsfakta")?.get("arbeidsgivere")?.mapNotNull {
                        if (it.get("inntektskilde") == null) return@mapNotNull null
                        SykepengegrunnlagsArbeidsgiver(
                            arbeidsgiver = it["arbeidsgiver"].asText(),
                            omregnetÅrsinntekt = it["omregnetÅrsinntekt"].asDouble(),
                            inntektskilde = Inntektsopplysningkilde.valueOf(it["inntektskilde"].asText()),
                            skjønnsfastsatt = it["skjønnsfastsatt"]?.asDouble(),
                        )
                    } ?: emptyList(),
            ),
        orgnummereMedRelevanteArbeidsforhold =
            jsonNode
                .path("Godkjenning")
                .path("orgnummereMedRelevanteArbeidsforhold")
                .takeUnless { it.isMissingNode || it.isNull }
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
    inntektskilderRepository: InntektskilderRepository,
    arbeidsforholdDao: ArbeidsforholdDao,
    egenAnsattDao: EgenAnsattDao,
    utbetalingDao: UtbetalingDao,
    vergemålDao: VergemålDao,
    åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    risikovurderingDao: RisikovurderingDao,
    påVentDao: PåVentDao,
    overstyringDao: OverstyringDao,
    automatiseringDao: AutomatiseringDao,
    oppgaveDao: OppgaveDao,
    avviksvurderingDao: AvviksvurderingDao,
    oppgaveService: OppgaveService,
    godkjenningMediator: GodkjenningMediator,
    totrinnsvurderingService: TotrinnsvurderingService,
    person: Person,
) : MacroCommand() {
    private val sykefraværstilfelle = person.sykefraværstilfelle(behovData.vedtaksperiodeId)
    private val inntektskilder =
        inntektskilderRepository.finnInntektskilder(
            fødselsnummer = behovData.fødselsnummer,
            andreOrganisasjonsnumre = behovData.orgnummereMedRelevanteArbeidsforhold + behovData.organisasjonsnummer,
        ).gjenopprett()
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
                førsteKjenteDagFinner = førsteKjenteDagFinner,
                personDao = personDao,
                inntektskilder = inntektskilder,
                inntektskilderRepository = inntektskilderRepository,
                arbeidsforholdDao = arbeidsforholdDao,
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
                spleisSykepengegrunnlangsfakta = behovData.spleisSykepengegrunnlagsfakta,
            ),
            VurderAutomatiskAvvisning(
                personDao = personDao,
                vergemålDao = vergemålDao,
                godkjenningMediator = godkjenningMediator,
                utbetaling = utbetaling,
                godkjenningsbehov = behovData,
                sykefraværstilfelle = sykefraværstilfelle,
            ),
            VurderAutomatiskInnvilgelse(
                automatisering = automatisering,
                godkjenningMediator = godkjenningMediator,
                utbetaling = utbetaling,
                sykefraværstilfelle = sykefraværstilfelle,
                godkjenningsbehov = behovData,
                automatiseringDao = automatiseringDao,
                oppgaveService = oppgaveService,
            ),
            OpprettSaksbehandleroppgave(
                behovData = behovData,
                oppgaveService = oppgaveService,
                automatisering = automatisering,
                personDao = personDao,
                risikovurderingDao = risikovurderingDao,
                egenAnsattDao = egenAnsattDao,
                utbetalingtype = behovData.utbetalingtype,
                sykefraværstilfelle = sykefraværstilfelle,
                utbetaling = utbetaling,
                vergemålDao = vergemålDao,
                påVentDao = påVentDao,
            ),
            VurderBehovForTotrinnskontroll(
                fødselsnummer = behovData.fødselsnummer,
                vedtaksperiodeId = behovData.vedtaksperiodeId,
                oppgaveService = oppgaveService,
                overstyringDao = overstyringDao,
                totrinnsvurderingService = totrinnsvurderingService,
                sykefraværstilfelle = sykefraværstilfelle,
                spleisVedtaksperioder = behovData.spleisVedtaksperioder,
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
    førsteKjenteDagFinner: () -> LocalDate,
    personDao: PersonDao,
    inntektskilder: List<no.nav.helse.modell.Inntektskilde>,
    inntektskilderRepository: InntektskilderRepository,
    arbeidsforholdDao: ArbeidsforholdDao,
) : MacroCommand() {
    override val commands: List<Command> =
        listOf(
            OppdaterPersonCommand(
                fødselsnummer = fødselsnummer,
                førsteKjenteDagFinner = førsteKjenteDagFinner,
                personDao = personDao,
            ),
            OpprettEllerOppdaterInntektskilder(
                inntektskilder = inntektskilder,
                inntektskilderRepository = inntektskilderRepository,
            ),
            OpprettEllerOppdaterArbeidsforhold(
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                arbeidsforholdDao = arbeidsforholdDao,
            ),
        )
}
