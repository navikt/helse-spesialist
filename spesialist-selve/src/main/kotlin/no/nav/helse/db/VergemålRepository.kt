package no.nav.helse.db

import no.nav.helse.modell.vergemal.VergemålOgFremtidsfullmakt

interface VergemålRepository {
    fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    )

    fun harVergemål(fødselsnummer: String): Boolean?
}
