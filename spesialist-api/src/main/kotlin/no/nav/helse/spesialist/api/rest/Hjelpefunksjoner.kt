package no.nav.helse.spesialist.api.rest

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

internal fun finnEllerOpprettTotrinnsvurdering(
    fodselsnummer: String,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
): Totrinnsvurdering =
    totrinnsvurderingRepository.finnAktivForPerson(fodselsnummer)
        ?: Totrinnsvurdering.ny(fødselsnummer = fodselsnummer).also(totrinnsvurderingRepository::lagre)

internal fun Saksbehandler.harTilgangTilPerson(
    identitetsnummer: Identitetsnummer,
    tilgangsgrupper: Set<Tilgangsgruppe>,
    transaksjon: SessionContext,
): Boolean {
    val person = transaksjon.personRepository.finn(identitetsnummer)
    if (person == null) {
        teamLogs.warn("Person med identitetsnummer $identitetsnummer ble ikke funnet")
        return false
    }
    if (!person.kanSeesAvSaksbehandlerMedGrupper(tilgangsgrupper)) {
        teamLogs.warn("Saksbehandler ${id.value} har ikke tilgang til person med identitetsnummer $identitetsnummer")
        return false
    }
    return true
}
