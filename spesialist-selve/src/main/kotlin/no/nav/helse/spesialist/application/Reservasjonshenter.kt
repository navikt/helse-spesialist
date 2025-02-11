package no.nav.helse.spesialist.application

interface Reservasjonshenter {
    suspend fun hentForPerson(fødselsnummer: String): ReservasjonDto?

    data class ReservasjonDto(
        val kanVarsles: Boolean,
        val reservert: Boolean,
    )
}
