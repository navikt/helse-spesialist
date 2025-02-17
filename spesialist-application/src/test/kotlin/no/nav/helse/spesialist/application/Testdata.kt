package no.nav.helse.spesialist.application

import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektsopplysningkilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.SykepengegrunnlagsArbeidsgiver
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

fun lagFødselsnummer() = nextLong(from = 10000_00000, until = 699999_99999).toString().padStart(11, '0')

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

private val etternavnListe =
    listOf("Diode", "Flom", "Damesykkel", "Undulat", "Bakgrunn", "Genser", "Fornøyelse", "Campingvogn", "Bakkeklaring")

private val organisasjonsnavnDel1 = listOf("NEPE", "KLOVNE", "BOBLEBAD-", "DUSTE", "SKIHOPP", "SMÅBARN", "SPANIA")
private val organisasjonsnavnDel2 = listOf("AVDELINGEN", "SENTERET", "FORUM", "KLUBBEN", "SNEKKERIET")

fun lagOrganisasjonsnavn() = organisasjonsnavnDel1.random() + organisasjonsnavnDel2.random()

fun lagSaksbehandlerident() = ('A'..'Z').random() + "${nextInt(from = 100_000, until = 999_999)}"
fun lagSaksbehandlernavn() = "${lagFornavn()} ${lagEtternavn()}"
fun lagEpostadresseFraFulltNavn(navn: String) = navn.split(" ").joinToString(".").lowercase() + "@nav.no"
fun lagTilfeldigSaksbehandlerepost() = lagEpostadresseFraFulltNavn(lagSaksbehandlernavn())

fun fødselsdato(): LocalDate {
    val end = LocalDate.now().minusYears(18)
    val start = end.minusYears(100)
    val randomDayInEpoch = nextLong(start.toEpochDay(), end.toEpochDay())
    return LocalDate.ofEpochDay(randomDayInEpoch)
}

fun lagFornavn() = fornavnListe.random()

fun lagEtternavn() = etternavnListe.random()

class TestPerson {
    val fødselsnummer: String = lagFødselsnummer()
    val aktørId: String = lagAktørId()
    private val arbeidsgivere = mutableMapOf<Int, TestArbeidsgiver>()
    private val arbeidsgiver1 = nyArbeidsgiver()
    private val arbeidsgiver2 = nyArbeidsgiver()
    private val vedtaksperiode1 = arbeidsgiver1.nyVedtaksperiode()
    private val vedtaksperiode2 = arbeidsgiver1.nyVedtaksperiode()
    val orgnummer: String = arbeidsgiver1.organisasjonsnummer
    val orgnummer2: String = arbeidsgiver2.organisasjonsnummer
    val vedtaksperiodeId1 = vedtaksperiode1.vedtaksperiodeId
    val vedtaksperiodeId2 = vedtaksperiode2.vedtaksperiodeId
    val utbetalingId1 = vedtaksperiode1.utbetalingId
    private val utbetalingId2 = vedtaksperiode2.utbetalingId

    override fun toString(): String {
        return "Testdatasett(fødselsnummer='$fødselsnummer', aktørId='$aktørId', orgnummer='$orgnummer', orgnummer2='$orgnummer2', vedtaksperiodeId1=$vedtaksperiodeId1, vedtaksperiodeId2=$vedtaksperiodeId2, utbetalingId1=$utbetalingId1, utbetalingId2=$utbetalingId2)"
    }

    fun nyArbeidsgiver() = TestArbeidsgiver(fødselsnummer).also {
        arbeidsgivere[arbeidsgivere.size] = it
    }
}

class TestArbeidsgiver(
    val fødselsnummer: String,
) {
    private val vedtaksperioder = mutableMapOf<Int, TestVedtaksperiode>()
    val organisasjonsnummer = lagOrganisasjonsnummer()

    fun nyVedtaksperiode() = TestVedtaksperiode(fødselsnummer, organisasjonsnummer).also {
        vedtaksperioder[vedtaksperioder.size] = it
    }

    val Int.vedtaksperiode
        get() = vedtaksperioder[this] ?: throw IllegalArgumentException(
            "Vedtaksperiode med index $this for arbeidsgiver $organisasjonsnummer finnes ikke",
        )
}

class TestVedtaksperiode(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
) {
    val vedtaksperiodeId: UUID = UUID.randomUUID()
    val utbetalingId: UUID = UUID.randomUUID()
}

object Testdata {
    fun godkjenningsbehovData(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = lagFødselsnummer(),
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID = UUID.randomUUID(),
        tags: List<String> = emptyList(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        kanAvvises: Boolean = true,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        inntektsopplysningkilde: Inntektsopplysningkilde = Inntektsopplysningkilde.Arbeidsgiver,
        json: String = "{}",
    ): GodkjenningsbehovData {
        return GodkjenningsbehovData(
            id = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisVedtaksperioder = emptyList(),
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            avviksvurderingId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID(),
            tags = tags,
            periodeFom = 1 jan 2018,
            periodeTom = 31 jan 2018,
            periodetype = periodetype,
            førstegangsbehandling = true,
            utbetalingtype = utbetalingtype,
            kanAvvises = kanAvvises,
            inntektskilde = inntektskilde,
            orgnummereMedRelevanteArbeidsforhold = emptyList(),
            skjæringstidspunkt = 1 jan 2018,
            spleisSykepengegrunnlagsfakta = SpleisSykepengegrunnlagsfakta(
                arbeidsgivere = listOf(
                    SykepengegrunnlagsArbeidsgiver(
                        arbeidsgiver = organisasjonsnummer,
                        omregnetÅrsinntekt = 123456.7,
                        inntektskilde = inntektsopplysningkilde,
                        skjønnsfastsatt = null
                    )
                )
            ),
            json = json,
        )
    }

}

val tilgangsgrupper = SpeilTilgangsgrupper(
    env = Gruppe.__indreInnhold_kunForTest().values.associateWith { _ -> UUID.randomUUID().toString() }
)
