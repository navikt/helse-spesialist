package no.nav.helse.spesialist.application

import no.nav.helse.db.api.PersonApiDao

class UnimplementedPersonApiDao : PersonApiDao {

    override fun finnInfotrygdutbetalinger(fødselsnummer: String): String? {
        TODO("Not yet implemented")
    }
}
