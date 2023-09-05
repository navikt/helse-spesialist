package no.nav.helse.modell.totrinnsvurdering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.modell.Saksbehandler

interface TotrinnsvurderingVisitor {
    fun visitTotrinnsvurdering(
        vedtaksperiodeId: UUID,
        erRetur: Boolean,
        saksbehandler: Saksbehandler?,
        beslutter: Saksbehandler?,
        utbetalingIdRef: Long?,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime?
    ) {}
}