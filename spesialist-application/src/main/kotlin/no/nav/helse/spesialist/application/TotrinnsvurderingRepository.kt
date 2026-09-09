package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId

interface TotrinnsvurderingRepository {
    fun lagre(totrinnsvurdering: Totrinnsvurdering)

    fun finnOrNull(id: TotrinnsvurderingId): Totrinnsvurdering?

    fun finnAktivForPersonOrNull(fødselsnummer: String): Totrinnsvurdering?
}
