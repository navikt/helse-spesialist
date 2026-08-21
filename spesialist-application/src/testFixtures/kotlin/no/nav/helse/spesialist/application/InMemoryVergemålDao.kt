package no.nav.helse.spesialist.application

import no.nav.helse.db.VergemålDao
import no.nav.helse.db.VergemålOgFremtidsfullmakt

class InMemoryVergemålDao : VergemålDao {
    private data class Vergemål(
        val vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        val fullmakt: Boolean,
    )

    private val vergemål = mutableMapOf<String, Vergemål>()

    override fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    ) {
        vergemål[fødselsnummer] = Vergemål(vergemålOgFremtidsfullmakt, fullmakt)
    }

    override fun harVergemål(fødselsnummer: String): Boolean? = vergemål[fødselsnummer]?.vergemålOgFremtidsfullmakt?.harVergemål

    override fun harFullmakt(fødselsnummer: String): Boolean? = vergemål[fødselsnummer]?.fullmakt
}
