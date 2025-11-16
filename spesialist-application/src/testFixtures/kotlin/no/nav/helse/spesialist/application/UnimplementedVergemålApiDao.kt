package no.nav.helse.spesialist.application

import no.nav.helse.db.api.VergemålApiDao

class UnimplementedVergemålApiDao : VergemålApiDao {
    override fun harFullmakt(fødselsnummer: String): Boolean? {
        TODO("Not yet implemented")
    }
}
