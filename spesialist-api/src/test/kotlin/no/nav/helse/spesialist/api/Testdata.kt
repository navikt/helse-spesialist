package no.nav.helse.spesialist.api

import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

fun lagFødselsnummer() = nextLong(from = 10000_00000, until = 699999_99999).toString().padStart(11, '0')

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

fun lagSaksbehandlerident() = ('A'..'Z').random() + "${nextInt(from = 100_000, until = 999_999)}"
fun lagSaksbehandlernavn() = "${lagFornavn()} ${lagEtternavn()}"
fun lagEpostadresseFraFulltNavn(navn: String) = navn.split(" ").joinToString(".").lowercase() + "@nav.no"

fun lagFornavn() = fornavnListe.random()

fun lagEtternavn() = etternavnListe.random()