package no.nav.helse.spesialist.application

import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering

interface TotrinnsvurderingRepository {
    fun lagre(totrinnsvurdering: Totrinnsvurdering)

    fun finn(fødselsnummer: String): Totrinnsvurdering?
}
