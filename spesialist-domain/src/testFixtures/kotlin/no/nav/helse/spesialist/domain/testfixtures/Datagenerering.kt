package no.nav.helse.spesialist.domain.testfixtures

import java.time.LocalDate
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
