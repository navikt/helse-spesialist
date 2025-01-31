package no.nav.helse.db

import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering

interface TotrinnsvurderingRepository {
    fun finnTotrinnsvurdering(fødselsnummer: String): Totrinnsvurdering?
}
