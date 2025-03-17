package no.nav.helse.spesialist.application

import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import java.util.UUID

interface TotrinnsvurderingRepository {
    fun lagre(totrinnsvurdering: Totrinnsvurdering)

    fun finn(fødselsnummer: String): Totrinnsvurdering?

    @Deprecated("Skal fjernes, midlertidig i bruk for å tette et hull")
    fun finn(vedtaksperiodeId: UUID): Totrinnsvurdering?
}
