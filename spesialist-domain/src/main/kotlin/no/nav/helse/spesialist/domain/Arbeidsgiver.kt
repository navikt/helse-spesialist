package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDate

@JvmInline
value class ArbeidsgiverId(val value: Int)

class Arbeidsgiver private constructor(
    id: ArbeidsgiverId?,
    val identifikator: Identifikator,
    navn: Navn?,
) : AggregateRoot<ArbeidsgiverId>(id) {
    var navn: Navn? = navn
        private set

    fun navnIkkeOppdatertSiden(dato: LocalDate) = navn?.ikkeOppdatertSiden(dato) ?: true

    fun oppdaterMedNavn(navn: String) {
        this.navn =
            Navn(
                navn = navn,
                sistOppdatertDato = LocalDate.now(),
            )
    }

    object Factory {
        fun ny(identifikator: Identifikator) =
            Arbeidsgiver(
                id = null,
                identifikator = identifikator,
                navn = null,
            )

        fun fraLagring(
            id: ArbeidsgiverId,
            identifikator: Identifikator,
            navn: Navn?,
        ) = Arbeidsgiver(
            id = id,
            identifikator = identifikator,
            navn = navn,
        )
    }

    data class Navn(
        val navn: String,
        val sistOppdatertDato: LocalDate,
    ) : ValueObject {
        fun ikkeOppdatertSiden(dato: LocalDate) = sistOppdatertDato.isBefore(dato)
    }

    sealed interface Identifikator : ValueObject {
        data class Fødselsnummer(val fødselsnummer: String) : Identifikator

        data class Organisasjonsnummer(val organisasjonsnummer: String) : Identifikator

        companion object {
            fun fraString(string: String): Identifikator =
                if (string.length == 9) {
                    Organisasjonsnummer(string)
                } else {
                    Fødselsnummer(string)
                }
        }
    }
}
