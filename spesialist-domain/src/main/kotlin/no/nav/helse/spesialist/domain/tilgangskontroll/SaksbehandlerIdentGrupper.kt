package no.nav.helse.spesialist.domain.tilgangskontroll

class SaksbehandlerIdentGruppe(
    vararg identer: String,
) {
    private val uppercaseIdenter = identer.map(String::uppercase).toSet()

    fun inneholder(ident: String): Boolean = uppercaseIdenter.contains(ident.uppercase())
}

object SaksbehandlerIdentGrupper {
    val COACHER =
        SaksbehandlerIdentGruppe(
            "B164848",
            "F131883",
            "F158061",
            "G155258",
            "H160235",
            "K162139",
            "S157539",
            "S165568",
            "V149621",
        )
}
