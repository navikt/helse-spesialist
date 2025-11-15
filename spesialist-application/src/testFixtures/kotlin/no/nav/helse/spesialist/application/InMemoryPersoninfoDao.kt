package no.nav.helse.spesialist.application

import no.nav.helse.db.api.PersoninfoDao

class InMemoryPersoninfoDao : PersoninfoDao {
    override fun hentPersoninfo(fødselsnummer: String): PersoninfoDao.Personinfo? {
        TODO("Not yet implemented")
    }
}
