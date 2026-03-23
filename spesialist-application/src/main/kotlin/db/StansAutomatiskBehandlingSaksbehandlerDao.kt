package no.nav.helse.db

interface StansAutomatiskBehandlingSaksbehandlerDao {
    fun lagreStans(fødselsnummer: String)

    fun opphevStans(fødselsnummer: String)

    fun erStanset(fødselsnummer: String): Boolean
}
