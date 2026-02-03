package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle.SELVSTENDIG_NÆRINGSDRIVENDE_BETA
import java.util.UUID

class TilgangsgrupperTilBrukerroller(
    val næringsdrivendeBeta: List<UUID>,
    val beslutter: List<UUID>,
    val egenAnsatt: List<UUID>,
    val kode7: List<UUID>,
    val stikkprøve: List<UUID>,
    val utvikler: List<UUID>,
    val saksbehandler: List<UUID>,
    val lesetilgang: List<UUID>,
) {
    fun finnBrukerrollerFraTilgangsgrupper(tilgangsgrupper: Collection<UUID>): Set<Brukerrolle> {
        val roller = mutableSetOf<Brukerrolle>()
        if (tilgangsgrupper.any { it in næringsdrivendeBeta }) {
            roller.add(SELVSTENDIG_NÆRINGSDRIVENDE_BETA)
        }
        if (tilgangsgrupper.any { it in beslutter }) {
            roller.add(Brukerrolle.BESLUTTER)
        }
        if (tilgangsgrupper.any { it in egenAnsatt }) {
            roller.add(Brukerrolle.EGEN_ANSATT)
        }
        if (tilgangsgrupper.any { it in kode7 }) {
            roller.add(Brukerrolle.KODE_7)
        }
        if (tilgangsgrupper.any { it in stikkprøve }) {
            roller.add(Brukerrolle.STIKKPRØVE)
        }
        if (tilgangsgrupper.any { it in utvikler }) {
            roller.add(Brukerrolle.UTVIKLER)
        }
        if (tilgangsgrupper.any { it in saksbehandler }) {
            roller.add(Brukerrolle.SAKSBEHANDLER)
        }
        if (tilgangsgrupper.any { it in lesetilgang }) {
            roller.add(Brukerrolle.LESETILGANG)
        }
        return roller
    }

    fun alleUuider(): Set<UUID> =
        næringsdrivendeBeta.toSet() +
            beslutter.toSet() +
            egenAnsatt.toSet() +
            kode7.toSet() +
            stikkprøve.toSet() +
            utvikler.toSet() +
            saksbehandler.toSet() +
            lesetilgang.toSet()
}
