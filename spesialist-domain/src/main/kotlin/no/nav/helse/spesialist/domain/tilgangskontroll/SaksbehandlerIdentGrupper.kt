package no.nav.helse.spesialist.domain.tilgangskontroll

import no.nav.helse.spesialist.domain.NAVIdent

class SaksbehandlerIdentGruppe(
    vararg identer: String,
) {
    private val uppercaseIdenter = identer.map(String::uppercase).toSet()

    fun inneholder(ident: NAVIdent): Boolean = uppercaseIdenter.contains(ident.value.uppercase())
}

object SaksbehandlerIdentGrupper {
    val TILGANG_TIL_SN =
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
            "F160464",
            "M136300",
            "A148751",
            "S161635",
        )
}
