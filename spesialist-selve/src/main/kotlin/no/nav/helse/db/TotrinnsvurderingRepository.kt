package no.nav.helse.db

import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering

interface TotrinnsvurderingRepository {
    fun finnTotrinnsvurdering(
        fødselsnummer: String,
        tilgangskontroll: Tilgangskontroll,
    ): Totrinnsvurdering?

    fun lagre(
        totrinnsvurdering: Totrinnsvurdering,
        fødselsnummer: String,
        tilgangskontroll: Tilgangskontroll,
    )
}
