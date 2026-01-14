package no.nav.helse.spesialist.application

interface PersonKlargjoresDao {
    fun klargjøringPågår(fødselsnummer: String): Boolean

    fun personKlargjøres(fødselsnummer: String)

    fun personKlargjort(fødselsnummer: String)
}
