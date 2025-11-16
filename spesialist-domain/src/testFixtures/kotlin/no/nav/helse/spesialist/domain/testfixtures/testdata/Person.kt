package no.nav.helse.spesialist.domain.testfixtures.testdata

import no.nav.helse.spesialist.domain.EgenAnsattStatus
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.Personinfo.Adressebeskyttelse
import no.nav.helse.spesialist.domain.Personinfo.Kjønn
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

fun lagPerson(
    fødselsdato: LocalDate? = lagFødselsdato(),
    kjønn: Kjønn? = Kjønn.entries.random(),
    identitetsnummer: Identitetsnummer = lagIdentitetsnummer(
        fødselsdato = fødselsdato ?: lagFødselsdato(),
        mann = kjønn == Kjønn.Mann
    ),
    aktørId: String = lagAktørId(),
    adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
    info: Personinfo? = lagPersoninfo(
        fødselsdato = fødselsdato,
        kjønn = kjønn,
        adressebeskyttelse = adressebeskyttelse
    ),
    erEgenAnsatt: Boolean? = false,
): Person = Person.Factory.ny(
    identitetsnummer = identitetsnummer,
    aktørId = aktørId,
    info = info,
    egenAnsattStatus = erEgenAnsatt?.let {
        EgenAnsattStatus(
            erEgenAnsatt = it,
            oppdatertTidspunkt = Instant.now(),
        )
    }
)

fun lagPersoninfo(
    fornavn: String = lagFornavn(),
    mellomnavn: String? = lagMellomnavnOrNull(),
    etternavn: String = lagEtternavn(),
    fødselsdato: LocalDate? = lagFødselsdato(),
    kjønn: Kjønn? = Kjønn.entries.random(),
    adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
): Personinfo = Personinfo(
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fødselsdato = fødselsdato,
    kjønn = kjønn,
    adressebeskyttelse = adressebeskyttelse,
)

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

fun lagFornavn() = fornavnListe.random()

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

fun lagMellomnavn() = mellomnavnListe.random()
fun lagMellomnavnOrNull() = if (Math.random() < 0.5) lagMellomnavn() else null

private val etternavnListe = listOf(
    "Diode",
    "Flom",
    "Damesykkel",
    "Undulat",
    "Bakgrunn",
    "Genser",
    "Fornøyelse",
    "Campingvogn",
    "Bakkeklaring"
)

fun lagEtternavn() = etternavnListe.random()

fun lagAktørId() = Random.nextLong(from = 1_000_000_000_000, until = 1_000_099_999_999).toString()

fun lagIdentitetsnummer() = lagIdentitetsnummer(fødselsdato = lagFødselsdato(), mann = Math.random() <= 0.5)
fun lagFødselsnummer() = lagFødselsnummer(fødselsdato = lagFødselsdato(), mann = Math.random() <= 0.5)
fun lagDNummer() = lagDNummer(fødselsdato = lagFødselsdato(), mann = Math.random() <= 0.5)

fun lagIdentitetsnummer(fødselsdato: LocalDate, mann: Boolean): Identitetsnummer =
    Identitetsnummer.fraString(
        if (Math.random() < 0.9) {
            lagFødselsnummer(fødselsdato, mann)
        } else {
            lagDNummer(fødselsdato, mann)
        }
    )

fun lagFødselsdato(): LocalDate {
    val end = LocalDate.now().minusYears(18)
    val start = end.minusYears(100)
    val randomDayInEpoch = Random.nextLong(start.toEpochDay(), end.toEpochDay())
    return LocalDate.ofEpochDay(randomDayInEpoch)
}

// Generer syntetisk fødselsnummer slik som Skattetaten også gjør - med 8 plusset på måneden i fødselsdatoen
// Ref. https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/test-for-konsumenter/
fun lagFødselsnummer(fødselsdato: LocalDate, mann: Boolean): String {
    val fødselsnummerFødselsdato = buildString {
        append(fødselsdato.dayOfMonth.toString().padStart(2, '0'))
        append(fødselsdato.month.value + 80)
        append((fødselsdato.year % 100).toString().padStart(2, '0'))
    }
    val personnummer = lagPersonnummerForFødselsnummer(
        fødselsnummerFødselsdato = fødselsnummerFødselsdato,
        fødselsår = fødselsdato.year,
        mann = mann
    )
    return fødselsnummerFødselsdato + personnummer
}

// Generer syntetisk D-nummer slik som Skattetaten også gjør - med 8 plusset på måneden i fødselsdatoen
// Ref. https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/test-for-konsumenter/
fun lagDNummer(fødselsdato: LocalDate, mann: Boolean): String {
    val dNummerFødselsdato = buildString {
        append(fødselsdato.dayOfMonth + 40)
        append(fødselsdato.month.value + 80)
        append((fødselsdato.year % 100).toString().padStart(2, '0'))
    }
    val personnummer = lagPersonnummerForDNummer(
        dNummerFødselsdato = dNummerFødselsdato,
        mann = mann
    )
    return dNummerFødselsdato + personnummer
}

private fun lagPersonnummerForFødselsnummer(fødselsnummerFødselsdato: String, fødselsår: Int, mann: Boolean): String {
    var individnummer: String
    var kontrollsiffer1: Int
    var kontrollsiffer2: Int
    do {
        do {
            // Tildel individnummer jf. https://lovdata.no/forskrift/2017-07-14-1201/§2-2-1
            individnummer = when (fødselsår) {
                in 1854..1899 -> Random.nextInt(from = 50, until = 75)
                in 1940..1999 if Math.random() < 0.5 -> Random.nextInt(from = 90, until = 100)
                in 1900..1999 -> Random.nextInt(from = 0, until = 50)
                in 2000..2039 -> Random.nextInt(from = 50, until = 100)
                else -> error("Det er ikke mulig å tildele personnummer til fødselsåret $fødselsår")
            }.toString().padStart(2, '0') + ((Random.nextInt(until = 5) * 2) + (if (mann) 1 else 0)).toString()
            kontrollsiffer1 = beregnKontrollsiffer1(fødselsnummerFødselsdato + individnummer)
        } while (kontrollsiffer1 > 9)
        kontrollsiffer2 = beregnKontrollsiffer2(fødselsnummerFødselsdato + individnummer + kontrollsiffer1)
    } while (kontrollsiffer2 > 9)

    return individnummer + kontrollsiffer1 + kontrollsiffer2
}

private fun lagPersonnummerForDNummer(dNummerFødselsdato: String, mann: Boolean): String {
    var individnummer: String
    var kontrollsiffer1: Int
    var kontrollsiffer2: Int
    do {
        do {
            // Individnummer i D-nummer tildeles fortløpende, ikke i serier
            individnummer = Random.nextInt(from = 0, until = 10).toString().padStart(2, '0') +
                    ((Random.nextInt(until = 5) * 2) + (if (mann) 1 else 0)).toString()
            kontrollsiffer1 = beregnKontrollsiffer1(dNummerFødselsdato + individnummer)
        } while (kontrollsiffer1 > 9)
        kontrollsiffer2 = beregnKontrollsiffer2(dNummerFødselsdato + individnummer + kontrollsiffer1)
    } while (kontrollsiffer2 > 9)

    return individnummer + kontrollsiffer1 + kontrollsiffer2
}

internal fun beregnKontrollsiffer1(sifre: String): Int =
    beregnKontrollsiffer(sifre, listOf(3, 7, 6, 1, 8, 9, 4, 5, 2))

internal fun beregnKontrollsiffer2(sifre: String): Int =
    beregnKontrollsiffer(sifre, listOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2))

private fun beregnKontrollsiffer(sifre: String, vekting: List<Int>): Int =
    11 - ((sifre).foldIndexed(0) { index, acc, char -> acc + vekting[index] * char.digitToInt() } % 11)
