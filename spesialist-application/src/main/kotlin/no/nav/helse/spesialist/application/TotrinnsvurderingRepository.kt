package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId

interface TotrinnsvurderingRepository {
    fun lagre(totrinnsvurdering: Totrinnsvurdering)

    fun finn(id: TotrinnsvurderingId): Totrinnsvurdering?

    fun finnAktivForPerson(fødselsnummer: String): Totrinnsvurdering?
}
