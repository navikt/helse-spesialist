package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDate

sealed interface ArbeidsgiverIdentifikator : ValueObject {
    data class Fødselsnummer(val fødselsnummer: String) : ArbeidsgiverIdentifikator

    data class Organisasjonsnummer(val organisasjonsnummer: String) : ArbeidsgiverIdentifikator

    companion object {
        fun fraString(string: String): ArbeidsgiverIdentifikator =
            if (string.length == 9) {
                Organisasjonsnummer(string)
            } else {
                Fødselsnummer(string)
            }
    }
}

class Arbeidsgiver private constructor(
    id: ArbeidsgiverIdentifikator,
    navn: Navn,
) : AggregateRoot<ArbeidsgiverIdentifikator>(id) {
    var navn: Navn = navn
        private set

    fun oppdaterMedNavn(navn: String) {
        this.navn =
            Navn(
                navn = navn,
                sistOppdatertDato = LocalDate.now(),
            )
    }

    object Factory {
        fun ny(
            id: ArbeidsgiverIdentifikator,
            navnString: String,
        ) = Arbeidsgiver(
            id = id,
            navn =
                Navn(
                    navn = navnString,
                    sistOppdatertDato = LocalDate.now(),
                ),
        )

        fun fraLagring(
            id: ArbeidsgiverIdentifikator,
            navn: Navn,
        ) = Arbeidsgiver(
            id = id,
            navn = navn,
        )
    }

    data class Navn(
        val navn: String,
        val sistOppdatertDato: LocalDate,
    ) : ValueObject {
        fun ikkeOppdatertSiden(dato: LocalDate) = sistOppdatertDato.isBefore(dato)
    }
}
