package no.nav.helse.spesialist.domain.testfixtures

import no.nav.helse.Varselvurdering
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.Utfall
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
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.Saksbehandler
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

fun lagFødselsnummer() = nextLong(from = 10000_00000, until = 319999_99999).toString().padStart(11, '0')

fun lagDnummer() = nextLong(from = 40000_00000, until = 719999_99999).toString().padStart(11, '0')

fun lagAktørId() = nextLong(from = 1_000_000_000_000, until = 1_000_099_999_999).toString()

fun lagOrganisasjonsnummer() = nextLong(from = 800_000_000, until = 999_999_999).toString()

private val fornavnListe = listOf(
    "Måteholden",
    "Dypsindig",
    "Ultrafiolett",
    "Urettferdig",
    "Berikende",
    "Upresis",
    "Stridlynt",
    "Rund",
    "Internasjonal"
)
private val mellomnavnListe = listOf(
    "Lysende",
    "Spennende",
    "Tidløs",
    "Hjertelig",
    "Storslått",
    "Sjarmerende",
    "Uforutsigbar",
    "Behagelig",
    "Robust",
    "Sofistikert",
)

private val etternavnListe =
    listOf("Diode", "Flom", "Damesykkel", "Undulat", "Bakgrunn", "Genser", "Fornøyelse", "Campingvogn", "Bakkeklaring")

private val organisasjonsnavnDel1 = listOf("NEPE", "KLOVNE", "BOBLEBAD-", "DUSTE", "SKIHOPP", "SMÅBARN", "SPANIA")
private val organisasjonsnavnDel2 = listOf("AVDELINGEN", "SENTERET", "FORUM", "KLUBBEN", "SNEKKERIET")

fun lagOrganisasjonsnavn() = organisasjonsnavnDel1.random() + organisasjonsnavnDel2.random()

fun lagSaksbehandlerOid() = SaksbehandlerOid(UUID.randomUUID())
fun lagSaksbehandlerident() = ('A'..'Z').random() + "${nextInt(from = 100_000, until = 999_999)}"
fun lagSaksbehandlernavn() = listOfNotNull(lagFornavn(), lagMellomnavnOrNull(), lagEtternavn()).joinToString(separator = " ")
fun lagEpostadresseFraFulltNavn(fulltNavn: String) = fulltNavn.split(" ").joinToString(".").lowercase() + "@nav.no"
fun lagTilfeldigSaksbehandlerepost() = lagEpostadresseFraFulltNavn(lagSaksbehandlernavn())

fun fødselsdato(): LocalDate {
    val end = LocalDate.now().minusYears(18)
    val start = end.minusYears(100)
    val randomDayInEpoch = nextLong(start.toEpochDay(), end.toEpochDay())
    return LocalDate.ofEpochDay(randomDayInEpoch)
}

fun lagFornavn() = fornavnListe.random()

fun lagMellomnavnOrNull() = (mellomnavnListe  + List(10) { null }).shuffled().random()
fun lagMellomnavn() = mellomnavnListe.random()

fun lagEtternavn() = etternavnListe.random()

fun lagEnOppgave(behandlingId: UUID): Oppgave = Oppgave.ny(
    id = nextLong(),
    førsteOpprettet = LocalDateTime.now(),
    vedtaksperiodeId = UUID.randomUUID(),
    behandlingId = behandlingId,
    utbetalingId = UUID.randomUUID(),
    hendelseId = UUID.randomUUID(),
    kanAvvises = true,
    egenskaper = emptySet(),
)

fun lagEnSaksbehandler(): Saksbehandler = lagSaksbehandler()

fun lagSaksbehandler(
    id: SaksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
    navn: String = lagSaksbehandlernavn(),
    epost: String = lagEpostadresseFraFulltNavn(navn),
    ident: String = lagSaksbehandlerident(),
): Saksbehandler = Saksbehandler(
    id = id,
    navn = navn,
    epost = epost,
    ident = ident
)

fun lagEnBehandling(
    id: UUID = UUID.randomUUID(),
    spleisBehandlingId: UUID = UUID.randomUUID(),
    utbetalingId: UtbetalingId = UtbetalingId(UUID.randomUUID()),
    vedtaksperiodeId: VedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
    tilstand: Behandling.Tilstand = Behandling.Tilstand.KlarTilBehandling,
    tags: Set<String> = setOf("Innvilget"),
    yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
    skjæringstidspunkt: LocalDate = 1.jan(2018),
): Behandling = lagBehandling(
    id = BehandlingUnikId(id),
    spleisBehandlingId = SpleisBehandlingId(spleisBehandlingId),
    vedtaksperiodeId = vedtaksperiodeId,
    utbetalingId = utbetalingId,
    tags = tags,
    tilstand = tilstand,
    søknadIder = emptySet(),
    fom = 1.jan(2018),
    tom = 31.jan(2018),
    skjæringstidspunkt = skjæringstidspunkt,
    yrkesaktivitetstype = yrkesaktivitetstype
)

fun lagBehandling(
    id: BehandlingUnikId = BehandlingUnikId(UUID.randomUUID()),
    spleisBehandlingId: SpleisBehandlingId? = SpleisBehandlingId(UUID.randomUUID()),
    vedtaksperiodeId: VedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID()),
    utbetalingId: UtbetalingId? = UtbetalingId(UUID.randomUUID()),
    tags: Set<String> = setOf("Behandling tag 1", "Behandling tag 2"),
    tilstand: Behandling.Tilstand = Behandling.Tilstand.KlarTilBehandling,
    fom: LocalDate = LocalDate.now().minusMonths(3).withDayOfMonth(1),
    tom: LocalDate = fom.withDayOfMonth(YearMonth.from(fom).lengthOfMonth()),
    skjæringstidspunkt: LocalDate = fom,
    yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
    søknadIder: Set<UUID> = emptySet(),
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
    søknadIder = søknadIder,
)

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

fun lagEtVarsel(
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

fun lagEnVarseldefinisjon(
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

fun lagEnVedtaksperiode(
    vedtaksperiodeId: UUID = UUID.randomUUID(),
    fødselsnummer: String = lagFødselsnummer(),
    organisasjonsnummer: String = lagOrganisasjonsnummer(),
    forkastet: Boolean = false,
): Vedtaksperiode = lagVedtaksperiode(
    id = VedtaksperiodeId(vedtaksperiodeId),
    identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
    organisasjonsnummer = organisasjonsnummer,
    forkastet = forkastet
)

fun lagVedtakBegrunnelse(
    spleisBehandlingId: SpleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
    tekst: String = "Her er en begrunnelsestekst",
    utfall: Utfall = Utfall.entries.random(),
    saksbehandlerOid: SaksbehandlerOid = lagSaksbehandlerOid(),
) = VedtakBegrunnelse.ny(
    spleisBehandlingId = spleisBehandlingId,
    tekst = tekst,
    utfall = utfall,
    saksbehandlerOid = saksbehandlerOid,
)

fun lagPerson(
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
    aktørId: String = lagAktørId(),
    adressebeskyttelse: Personinfo.Adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert
): Person = Person.Factory.ny(
    identitetsnummer = identitetsnummer,
    aktørId = aktørId,
    info = Personinfo(
        fornavn = lagFornavn(),
        mellomnavn = lagMellomnavn(),
        etternavn = lagEtternavn(),
        fødselsdato = fødselsdato(),
        kjønn = Personinfo.Kjønn.entries.random(),
        adressebeskyttelse = adressebeskyttelse
    )
)

fun lagIdentitetsnummer(): Identitetsnummer = Identitetsnummer.fraString(lagFødselsnummer())

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
