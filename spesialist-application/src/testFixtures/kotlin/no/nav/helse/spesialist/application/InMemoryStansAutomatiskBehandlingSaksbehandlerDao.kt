package no.nav.helse.spesialist.application

import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao

class InMemoryStansAutomatiskBehandlingSaksbehandlerDao : StansAutomatiskBehandlingSaksbehandlerDao {
    private val data = mutableListOf<String>()

    override fun lagreStans(fødselsnummer: String) {
        data.add(fødselsnummer)
    }

    override fun opphevStans(fødselsnummer: String) {
        data.removeIf { it == fødselsnummer }
    }

    override fun erStanset(fødselsnummer: String): Boolean {
        return data.any { it == fødselsnummer }
    }
}
