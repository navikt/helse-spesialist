package no.nav.helse.db

import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import java.util.UUID

interface TotrinnsvurderingRepository {
    fun finnTotrinnsvurdering(fødselsnummer: String): Totrinnsvurdering?

    @Deprecated("Skal fjernes, midlertidig i bruk for å tette et hull")
    fun finnTotrinnsvurdering(vedtaksperiodeId: UUID): Totrinnsvurdering?

    fun lagre(
        totrinnsvurdering: Totrinnsvurdering,
        fødselsnummer: String,
    )
}
