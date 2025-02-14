package no.nav.helse.db

interface VergemålDao {
    fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    )

    fun harVergemål(fødselsnummer: String): Boolean?

    fun harFullmakt(fødselsnummer: String): Boolean?
}

data class VergemålOgFremtidsfullmakt(
    val harVergemål: Boolean,
    val harFremtidsfullmakter: Boolean,
)
