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

    fun harTilgangTilOppgaveMedEgenskaper(
        egenskaper: Set<Egenskap>,
        saksbehandler: Saksbehandler,
    ): Boolean =
        harTilgangTilOppgaveMedEgenskaper(
            egenskaper = egenskaper,
            saksbehandler = saksbehandler,
            tilgangsgrupper = hentTilgangsgrupper(saksbehandler),
        )

    fun harTilgangTilOppgaveMedEgenskaper(
        egenskaper: Set<Egenskap>,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): Boolean =
        egenskaper.all {
            harTilgangTilOppgaveMedEgenskap(
                egenskap = it,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
            )
        }

    fun harTilgangTilOppgaveMedEgenskap(
        egenskap: Egenskap,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ) = when (egenskap) {
        STRENGT_FORTROLIG_ADRESSE -> false // Ingen skal ha tilgang til disse i Speil foreløpig
        EGEN_ANSATT -> Tilgangsgruppe.SKJERMEDE in tilgangsgrupper
        FORTROLIG_ADRESSE -> Tilgangsgruppe.KODE7 in tilgangsgrupper
        BESLUTTER -> Tilgangsgruppe.BESLUTTER in tilgangsgrupper
        STIKKPRØVE -> Tilgangsgruppe.STIKKPRØVE in tilgangsgrupper
        else -> true
    }

    private fun hentTilgangsgrupper(saksbehandler: Saksbehandler): Set<Tilgangsgruppe> =
        runBlocking {
            tilgangsgruppehenter.hentTilgangsgrupper(saksbehandler.id().value)
        }
}
