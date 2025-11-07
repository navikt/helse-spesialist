package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.ValueObject

sealed interface Identitetsnummer : ValueObject {
    val value: String

    companion object {
        fun fraString(identitetsnummer: String): Identitetsnummer {
            val førsteSiffer = identitetsnummer.first().digitToInt()
            return when (førsteSiffer) {
                in 4..7 -> DNummer(identitetsnummer)
                in 0..3 -> Fødselsnummer(identitetsnummer)
                else -> error("Identitetsnummeret er ikke gyldig")
            }
        }
    }
}

data class Fødselsnummer(
    override val value: String,
) : Identitetsnummer

data class DNummer(
    override val value: String,
) : Identitetsnummer
