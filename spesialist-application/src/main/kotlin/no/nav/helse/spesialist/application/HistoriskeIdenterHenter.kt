package no.nav.helse.spesialist.application

fun interface HistoriskeIdenterHenter {
    fun hentHistoriskeIdenter(ident: String): List<String>
}
