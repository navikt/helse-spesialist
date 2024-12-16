package no.nav.helse.modell

import java.time.LocalDate
import java.time.LocalDateTime

open class ArbeidsforholdDto(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
) {
    open fun måOppdateres(): Boolean = true
}

class KomplettArbeidsforholdDto(
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val stillingstittel: String,
    val stillingsprosent: Int,
    val oppdatert: LocalDateTime = LocalDateTime.now(),
    fødselsnummer: String,
    organisasjonsnummer: String,
) : ArbeidsforholdDto(
        fødselsnummer,
        organisasjonsnummer,
    ) {
    override fun måOppdateres(): Boolean {
        return oppdatert <= LocalDateTime.now().minusDays(1)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is KomplettArbeidsforholdDto &&
                javaClass == other.javaClass &&
                fødselsnummer.equals(other.fødselsnummer) &&
                organisasjonsnummer.equals(other.organisasjonsnummer) &&
                startdato.equals(other.startdato) &&
                sluttdato == other.sluttdato &&
                stillingstittel.equals(other.stillingstittel) &&
                stillingsprosent.equals(other.stillingsprosent) &&
                oppdatert.withNano(0) == other.oppdatert.withNano(0)

        )

    override fun hashCode(): Int {
        var result = fødselsnummer.hashCode()
        result = 31 * result + organisasjonsnummer.hashCode()
        result = 31 * result + startdato.hashCode()
        sluttdato?.also { result = 31 * result + sluttdato.hashCode() }
        result = 31 * result + stillingstittel.hashCode()
        result = 31 * result + stillingsprosent.hashCode()
        result = 31 * result + oppdatert.withNano(0).hashCode()
        return result
    }
}
