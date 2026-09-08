package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import java.util.*

class TilgangsgrupperTilBrukerroller(
    val næringsdrivendeBeta: List<UUID>,
    val beslutter: List<UUID>,
    val egenAnsatt: List<UUID>,
    val kode7: List<UUID>,
    val stikkprøve: List<UUID>,
    val utvikler: List<UUID>,
    val dialogmelding: List<UUID>,
    val porteføljestyring: List<UUID>,
    val graderteAndreYtelser: List<UUID>,
) {
    fun finnBrukerrollerFraTilgangsgrupper(tilgangsgrupper: Collection<UUID>): Set<Brukerrolle> = Brukerrolle.entries.filter { rolle -> tilgangsgrupper.any { it in uuiderFor(rolle) } }.toSet()

    private fun uuiderFor(rolle: Brukerrolle): List<UUID> =
        when (rolle) {
            Brukerrolle.SelvstendigNæringsdrivendeBeta -> næringsdrivendeBeta
            Brukerrolle.Beslutter -> beslutter
            Brukerrolle.EgenAnsatt -> egenAnsatt
            Brukerrolle.Kode7 -> kode7
            Brukerrolle.Stikkprøve -> stikkprøve
            Brukerrolle.Utvikler -> utvikler
            Brukerrolle.Dialogmelding -> dialogmelding
            Brukerrolle.Porteføljestyring -> porteføljestyring
            Brukerrolle.GraderteAndreYtelser -> graderteAndreYtelser
        }

    fun alleUuider(): Set<UUID> = Brukerrolle.entries.flatMap(::uuiderFor).toSet()
}
