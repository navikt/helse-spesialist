package no.nav.helse.spesialist.domain.testfixtures

import no.nav.helse.Varselvurdering
import no.nav.helse.modell.oppgave.Inntektsforhold
import no.nav.helse.modell.oppgave.Mottaker
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgavetype
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random.Default.nextLong

fun lagOrganisasjonsnummer() = nextLong(from = 800_000_000, until = 999_999_999).toString()

private val organisasjonsnavnDel1 = listOf("NEPE", "KLOVNE", "BOBLEBAD-", "DUSTE", "SKIHOPP", "SMÅBARN", "SPANIA")
private val organisasjonsnavnDel2 = listOf("AVDELINGEN", "SENTERET", "FORUM", "KLUBBEN", "SNEKKERIET")
fun lagOrganisasjonsnavn() = organisasjonsnavnDel1.random() + organisasjonsnavnDel2.random()

fun lagAvviksvurderingMedEnArbeidsgiver(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    organisasjonsnummer: String = lagOrganisasjonsnummer(),
    skjæringstidspunkt: LocalDate = LocalDate.now().minusMonths(3).withDayOfMonth(1),
    omregnetÅrsinntekt: BigDecimal,
    innrapportertÅrsinntekt: BigDecimal,
    avviksprosent: BigDecimal = if (innrapportertÅrsinntekt >= omregnetÅrsinntekt) {
        innrapportertÅrsinntekt.setScale(4, RoundingMode.HALF_UP)
            .divide(omregnetÅrsinntekt)
            .subtract(BigDecimal.ONE)
            .multiply(BigDecimal.valueOf(100))
    } else {
        omregnetÅrsinntekt.setScale(4, RoundingMode.HALF_UP)
            .divide(innrapportertÅrsinntekt)
            .subtract(BigDecimal.ONE)
            .multiply(BigDecimal.valueOf(100))
    },
): Avviksvurdering = Avviksvurdering(
    unikId = UUID.randomUUID(),
    vilkårsgrunnlagId = UUID.randomUUID(),
    fødselsnummer = identitetsnummer.value,
    skjæringstidspunkt = skjæringstidspunkt,
    opprettet = LocalDateTime.now(),
    avviksprosent = avviksprosent.toDouble(),
    sammenligningsgrunnlag = Sammenligningsgrunnlag(
        totalbeløp = innrapportertÅrsinntekt.toDouble(),
        innrapporterteInntekter = listOf(
            InnrapportertInntekt(
                arbeidsgiverreferanse = organisasjonsnummer,
                inntekter = (1..12).map {
                    Inntekt(
                        årMåned = YearMonth.of(
                            skjæringstidspunkt.year - 1,
                            Month.of(it)
                        ),
                        beløp = (innrapportertÅrsinntekt / BigDecimal(12)).toDouble()
                    )
                }
            )
        )
    ),
    beregningsgrunnlag = Beregningsgrunnlag(
        totalbeløp = omregnetÅrsinntekt.toDouble(),
        omregnedeÅrsinntekter = listOf(
            OmregnetÅrsinntekt(
                arbeidsgiverreferanse = organisasjonsnummer,
                beløp = omregnetÅrsinntekt.toDouble()
            )
        )
    )
)

fun lagOppgave(behandlingId: SpleisBehandlingId, godkjenningsbehovId: UUID): Oppgave = Oppgave.ny(
    id = nextLong(),
    førsteOpprettet = LocalDateTime.now(),
    vedtaksperiodeId = UUID.randomUUID(),
    behandlingId = behandlingId.value,
    utbetalingId = UUID.randomUUID(),
    hendelseId = godkjenningsbehovId,
    kanAvvises = true,
    egenskaper = emptySet(),
    mottaker = Mottaker.UtbetalingTilArbeidsgiver,
    type = Oppgavetype.Søknad,
    inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
    inntektsforhold = Inntektsforhold.Arbeidstaker,
    periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
)

fun lagSpleisBehandlingId() = SpleisBehandlingId(UUID.randomUUID())

fun lagBehandlingUnikId() = BehandlingUnikId(UUID.randomUUID())

fun lagBehandling(
    id: BehandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
    spleisBehandlingId: SpleisBehandlingId? = SpleisBehandlingId(UUID.randomUUID()),
    vedtaksperiodeId: VedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
    utbetalingId: UtbetalingId? = UtbetalingId(UUID.randomUUID()),
    tags: Set<String> = setOf("Innvilget"),
    tilstand: Behandling.Tilstand = Behandling.Tilstand.KlarTilBehandling,
    fom: LocalDate = LocalDate.now().minusMonths(3).withDayOfMonth(1),
    tom: LocalDate = fom.withDayOfMonth(YearMonth.from(fom).lengthOfMonth()),
    skjæringstidspunkt: LocalDate = fom,
    yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
): Behandling = Behandling.fraLagring(
    id = id,
    spleisBehandlingId = spleisBehandlingId,
    vedtaksperiodeId = vedtaksperiodeId,
    utbetalingId = utbetalingId,
    tags = tags,
    tilstand = tilstand,
    fom = fom,
    tom = tom,
    skjæringstidspunkt = skjæringstidspunkt,
    yrkesaktivitetstype = yrkesaktivitetstype,
)

fun lagSkjønnsfastsattSykepengegrunnlag(
    saksbehandlerOid: SaksbehandlerOid,
    vedtaksperiodeId: VedtaksperiodeId,
    identitetsnummer: Identitetsnummer,
    aktørId: String,
    skjæringstidspunkt: LocalDate,
    organisasjonsnummer: String,
    skjønnsfastsattBeløp: BigDecimal,
    omregnetÅrsinntekt: BigDecimal
): SkjønnsfastsattSykepengegrunnlag = SkjønnsfastsattSykepengegrunnlag.ny(
    saksbehandlerOid = saksbehandlerOid,
    fødselsnummer = identitetsnummer.value,
    aktørId = aktørId,
    vedtaksperiodeId = vedtaksperiodeId.value,
    skjæringstidspunkt = skjæringstidspunkt,
    arbeidsgivere = listOf(
        SkjønnsfastsattArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            årlig = skjønnsfastsattBeløp.toDouble(),
            fraÅrlig = omregnetÅrsinntekt.toDouble(),
            årsak = "Skjønnsfastsettelse ved mer enn 25 % avvik (§ 8-30 andre avsnitt)",
            type = SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET,
            begrunnelseMal = "Begrunnelse fra mal her",
            begrunnelseFritekst = "Begrunnelse fra fritekst her",
            begrunnelseKonklusjon = "Begrunnelse fra konklusjon her",
            lovhjemmel = Lovhjemmel(
                paragraf = "8-30",
                ledd = "2",
                bokstav = null,
                lovverk = "folketrygdloven",
                lovverksversjon = "2019-01-01"
            ),
            initierendeVedtaksperiodeId = vedtaksperiodeId.value.toString()
        )
    ),
)

fun lagVedtaksperiodeId() = VedtaksperiodeId(UUID.randomUUID())
fun lagVedtaksperiode(
    id: VedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    organisasjonsnummer: String = lagOrganisasjonsnummer(),
    forkastet: Boolean = false,
): Vedtaksperiode = Vedtaksperiode(
    id = id,
    fødselsnummer = identitetsnummer.value,
    organisasjonsnummer = organisasjonsnummer,
    forkastet = forkastet
)

fun lagNotat(
    tekst: String = "Dette er et notat",
    type: NotatType = NotatType.Generelt,
    dialogRef: DialogId = DialogId(nextLong()),
    vedtaksperiodeId: UUID = lagVedtaksperiodeId().value,
    saksbehandlerOid: SaksbehandlerOid = lagSaksbehandler().id,
): Notat = Notat.Factory.ny(
    tekst = tekst,
    type = type,
    dialogRef = dialogRef,
    vedtaksperiodeId = vedtaksperiodeId,
    saksbehandlerOid = saksbehandlerOid,
)

fun lagVarselId() = VarselId(UUID.randomUUID())
fun lagVarsel(
    id: UUID = UUID.randomUUID(),
    behandlingUnikId: BehandlingUnikId,
    spleisBehandlingId: SpleisBehandlingId?,
    status: Varsel.Status = Varsel.Status.AKTIV,
    kode: String = "RV_IV_1",
    opprettet: LocalDateTime = LocalDateTime.now(),
    vurdering: Varselvurdering? = null,
): Varsel = Varsel.fraLagring(
    id = VarselId(id),
    spleisBehandlingId = spleisBehandlingId,
    behandlingUnikId = behandlingUnikId,
    status = status,
    kode = kode,
    opprettetTidspunkt = opprettet,
    vurdering = vurdering
)

fun lagVarseldefinisjonId() = VarseldefinisjonId(UUID.randomUUID())
fun lagVarseldefinisjon(
    id: UUID = UUID.randomUUID(),
    kode: String = "RV_IV_1",
    tittel: String = "Dette er en tittel",
    forklaring: String? = "Dette er en forklaring",
    handling: String? = "Dette er en handling",
): Varseldefinisjon = Varseldefinisjon.fraLagring(
    id = VarseldefinisjonId(id),
    kode = kode,
    tittel = tittel,
    forklaring = forklaring,
    handling = handling
)

fun lagVedtakBegrunnelse(
    spleisBehandlingId: SpleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
    tekst: String = "Her er en begrunnelsestekst",
    utfall: Utfall = Utfall.entries.random(),
    saksbehandlerOid: SaksbehandlerOid = lagSaksbehandler().id,
) = VedtakBegrunnelse.ny(
    spleisBehandlingId = spleisBehandlingId,
    tekst = tekst,
    utfall = utfall,
    saksbehandlerOid = saksbehandlerOid,
)
