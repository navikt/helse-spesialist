package no.nav.helse.spesialist.test

import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

fun lagFødselsnummer() = nextLong(from = 100000_00000, until = 699999_99999).toString()

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
) + List(10) { null }

private val etternavnListe =
    listOf("Diode", "Flom", "Damesykkel", "Undulat", "Bakgrunn", "Genser", "Fornøyelse", "Campingvogn", "Bakkeklaring")

private val organisasjonsnavnDel1 = listOf("NEPE", "KLOVNE", "BOBLEBAD-", "DUSTE", "SKIHOPP", "SMÅBARN", "SPANIA")
private val organisasjonsnavnDel2 = listOf("AVDELINGEN", "SENTERET", "FORUM", "KLUBBEN", "SNEKKERIET")

fun lagOrganisasjonsnavn() = organisasjonsnavnDel1.random() + organisasjonsnavnDel2.random()

fun lagSaksbehandlerident() = ('A'..'Z').random() + "${nextInt(from = 100_000, until = 999_999)}"

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
    val fornavn: String = fornavnListe.random()
    val mellomnavn: String? = mellomnavnListe.shuffled().random()
    val etternavn: String = etternavnListe.random()
    val kjønn = Kjønn.entries.toTypedArray().random()
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
    val utbetalingId2 = vedtaksperiode2.utbetalingId

    override fun toString(): String {
        return "Testdatasett(fødselsnummer='$fødselsnummer', aktørId='$aktørId', orgnummer='$orgnummer', orgnummer2='$orgnummer2', vedtaksperiodeId1=$vedtaksperiodeId1, vedtaksperiodeId2=$vedtaksperiodeId2, utbetalingId1=$utbetalingId1, utbetalingId2=$utbetalingId2)"
    }

    val Int.arbeidsgiver
        get() = arbeidsgivere[this - 1] ?: throw IllegalArgumentException("Arbeidsgiver med index $this finnes ikke")

    fun nyArbeidsgiver() = TestArbeidsgiver(fødselsnummer, aktørId).also {
        arbeidsgivere[arbeidsgivere.size] = it
    }
}

class TestArbeidsgiver(
    val fødselsnummer: String,
    val aktørId: String,
) {
    private val vedtaksperioder = mutableMapOf<Int, TestVedtaksperiode>()
    val organisasjonsnummer = lagOrganisasjonsnummer()
    val organisasjonsnavn = lagOrganisasjonsnavn()

    fun nyVedtaksperiode() = TestVedtaksperiode(fødselsnummer, aktørId, organisasjonsnummer).also {
        vedtaksperioder[vedtaksperioder.size] = it
    }

    val Int.vedtaksperiode
        get() = vedtaksperioder[this] ?: throw IllegalArgumentException(
            "Vedtaksperiode med index $this for arbeidsgiver $organisasjonsnummer finnes ikke",
        )
}

class TestVedtaksperiode(
    val fødselsnummer: String,
    val aktørId: String,
    val organisasjonsnummer: String,
) {
    val vedtaksperiodeId: UUID = UUID.randomUUID()
    val spleisBehandlingId: UUID = UUID.randomUUID()
    val utbetalingId: UUID = UUID.randomUUID()
}
