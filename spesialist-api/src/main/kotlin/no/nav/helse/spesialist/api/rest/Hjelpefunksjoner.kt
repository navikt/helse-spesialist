package no.nav.helse.spesialist.api.rest

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle

internal fun finnEllerOpprettTotrinnsvurdering(
    identitetsnummer: Identitetsnummer,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
): Totrinnsvurdering =
    totrinnsvurderingRepository.finnAktivForPerson(identitetsnummer.value)
        ?: Totrinnsvurdering.ny(fødselsnummer = identitetsnummer.value).also(totrinnsvurderingRepository::lagre)

internal fun Saksbehandler.harTilgangTilPerson(
    identitetsnummer: Identitetsnummer,
    brukerroller: Set<Brukerrolle>,
    transaksjon: SessionContext,
): Boolean {
    val person = transaksjon.personRepository.finn(identitetsnummer)
    if (person == null) {
        teamLogs.warn("Person med identitetsnummer $identitetsnummer ble ikke funnet")
        return false
    }
    if (!person.kanSeesAvSaksbehandlerMedGrupper(brukerroller)) {
        teamLogs.warn("Saksbehandler ${id.value} har ikke tilgang til person med identitetsnummer $identitetsnummer")
        return false
    }
    return true
}
