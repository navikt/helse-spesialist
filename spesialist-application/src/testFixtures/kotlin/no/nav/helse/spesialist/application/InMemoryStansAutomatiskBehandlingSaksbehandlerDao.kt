package no.nav.helse.spesialist.application

import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao

class InMemoryStansAutomatiskBehandlingSaksbehandlerDao : StansAutomatiskBehandlingSaksbehandlerDao {
    override fun lagreStans(fødselsnummer: String) {
        TODO("Not yet implemented")
    }

    override fun opphevStans(fødselsnummer: String) {
        TODO("Not yet implemented")
    }

    override fun erStanset(fødselsnummer: String): Boolean {
        TODO("Not yet implemented")
    }
}
