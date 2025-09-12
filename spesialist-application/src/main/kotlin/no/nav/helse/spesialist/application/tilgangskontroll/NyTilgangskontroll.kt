package no.nav.helse.spesialist.application.tilgangskontroll

import kotlinx.coroutines.runBlocking
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Egenskap.BESLUTTER
import no.nav.helse.modell.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.modell.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.modell.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.spesialist.domain.Saksbehandler

class NyTilgangskontroll(
    private val egenAnsattApiDao: EgenAnsattApiDao,
    private val personApiDao: PersonApiDao,
    private val tilgangsgruppehenter: Tilgangsgruppehenter,
) {
    fun harTilgangTilPerson(
        saksbehandlerTilganger: SaksbehandlerTilganger,
        fødselsnummer: String,
    ): Boolean = !manglerTilgang(egenAnsattApiDao, personApiDao, fødselsnummer, saksbehandlerTilganger)

    fun harTilgangTilOppgave(
        saksbehandler: Saksbehandler,
        egenskaper: List<Egenskap>,
    ): Boolean {
        if (egenskaper.isEmpty()) return true
        val tilgangsgrupper = hentTilgangsgrupper(saksbehandler)
        return egenskaper.all {
            harTilgangTilEgenskap(
                egenskap = it,
                saksbehandler = saksbehandler,
                grupper = tilgangsgrupper,
            )
        }
    }

    fun harTilgangTilEgenskap(
        egenskap: Egenskap,
        saksbehandler: Saksbehandler,
        grupper: Set<Gruppe>,
    ) = when (egenskap) {
        STRENGT_FORTROLIG_ADRESSE -> false // Ingen skal ha tilgang til disse i Speil foreløpig
        EGEN_ANSATT -> Gruppe.SKJERMEDE in grupper
        FORTROLIG_ADRESSE -> Gruppe.KODE7 in grupper
        BESLUTTER -> Gruppe.BESLUTTER in grupper
        STIKKPRØVE -> Gruppe.STIKKPRØVE in grupper
        else -> true
    }

    private fun hentTilgangsgrupper(saksbehandler: Saksbehandler): Set<Gruppe> =
        runBlocking {
            tilgangsgruppehenter.hentTilgangsgrupper(saksbehandler.id().value)
        }
}
