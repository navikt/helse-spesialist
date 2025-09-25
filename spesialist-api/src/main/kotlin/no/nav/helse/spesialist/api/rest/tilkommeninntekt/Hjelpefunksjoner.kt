package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.rest.HttpException
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.tilgangskontroll.PersonTilgangskontroll
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

internal fun finnEllerOpprettTotrinnsvurdering(
    fodselsnummer: String,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
): Totrinnsvurdering =
    totrinnsvurderingRepository.finnAktivForPerson(fodselsnummer)
        ?: Totrinnsvurdering.ny(fødselsnummer = fodselsnummer).also(totrinnsvurderingRepository::lagre)

internal fun bekreftTilgangTilPerson(
    fødselsnummer: String,
    saksbehandler: Saksbehandler,
    tilgangsgrupper: Set<Tilgangsgruppe>,
    transaksjon: SessionContext,
    feilSupplier: (String) -> HttpException,
) {
    if (!PersonTilgangskontroll.harTilgangTilPerson(
            tilgangsgrupper = tilgangsgrupper,
            fødselsnummer = fødselsnummer,
            egenAnsattDao = transaksjon.egenAnsattDao,
            personDao = transaksjon.personDao,
        )
    ) {
        throw feilSupplier("Saksbehandler mangler tilgang")
    }
}
