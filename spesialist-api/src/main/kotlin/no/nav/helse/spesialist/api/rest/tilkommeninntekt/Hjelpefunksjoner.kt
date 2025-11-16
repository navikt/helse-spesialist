package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

internal fun finnEllerOpprettTotrinnsvurdering(
    fodselsnummer: String,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
): Totrinnsvurdering =
    totrinnsvurderingRepository.finnAktivForPerson(fodselsnummer)
        ?: Totrinnsvurdering.ny(fødselsnummer = fodselsnummer).also(totrinnsvurderingRepository::lagre)

internal fun harTilgangTilPerson(
    fødselsnummer: String,
    saksbehandler: Saksbehandler,
    tilgangsgrupper: Set<Tilgangsgruppe>,
    transaksjon: SessionContext,
): Boolean =
    harTilgangTilPerson(
        identitetsnummer = Identitetsnummer.fraString(fødselsnummer),
        saksbehandler = saksbehandler,
        tilgangsgrupper = tilgangsgrupper,
        transaksjon = transaksjon,
    )

internal fun harTilgangTilPerson(
    identitetsnummer: Identitetsnummer,
    saksbehandler: Saksbehandler,
    tilgangsgrupper: Set<Tilgangsgruppe>,
    transaksjon: SessionContext,
): Boolean {
    val person = transaksjon.personRepository.finn(identitetsnummer)
    if (person == null) {
        sikkerlogg.warn("Person med identitetsnummer $identitetsnummer ble ikke funnet")
        return false
    }
    if (!person.kanSeesAvSaksbehandlerMedGrupper(tilgangsgrupper)) {
        sikkerlogg.warn("Saksbehandler ${saksbehandler.id.value} har ikke tilgang til person med identitetsnummer $identitetsnummer")
        return false
    }
    return true
}
