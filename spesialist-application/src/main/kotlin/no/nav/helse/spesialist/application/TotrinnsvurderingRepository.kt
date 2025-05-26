package no.nav.helse.spesialist.application

import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId

interface TotrinnsvurderingRepository {
    fun lagre(totrinnsvurdering: Totrinnsvurdering)

    fun finn(id: TotrinnsvurderingId): Totrinnsvurdering?

    fun finn(fødselsnummer: String): Totrinnsvurdering?
}
