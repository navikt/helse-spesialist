package no.nav.helse.db

import no.nav.helse.modell.kommando.MinimalPersonDto

interface PersonRepository {
    fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto?

    fun lagreMinimalPerson(minimalPerson: MinimalPersonDto)
}
