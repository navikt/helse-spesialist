package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer

fun interface AlleIdenterHenter {
    fun hentAlleIdenter(identitetsnummer: Identitetsnummer): List<Ident>

    data class Ident(
        val ident: String,
        val type: IdentType,
        val gjeldende: Boolean,
    )

    enum class IdentType {
        FOLKEREGISTERIDENT,
        AKTORID,
        NPID,
    }
}
