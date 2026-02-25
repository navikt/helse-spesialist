package no.nav.helse.spesialist.application

fun interface AlleIdenterHenter {
    fun hentAlleIdenter(ident: String): List<Ident>

    data class Ident(
        val ident: String,
        val type: IdentType,
    )

    enum class IdentType {
        FOLKEREGISTERIDENT,
        AKTORID,
        NPID,
    }
}
