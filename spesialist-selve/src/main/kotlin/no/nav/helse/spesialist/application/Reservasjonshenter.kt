package no.nav.helse.spesialist.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

interface Reservasjonshenter {
    suspend fun hentForPerson(fødselsnummer: String): ReservasjonDto?

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ReservasjonDto(
        val kanVarsles: Boolean,
        val reservert: Boolean,
    )
}
