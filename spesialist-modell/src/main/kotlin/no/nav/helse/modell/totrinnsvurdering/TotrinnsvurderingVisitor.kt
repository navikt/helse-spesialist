package no.nav.helse.modell.totrinnsvurdering

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.saksbehandler.Saksbehandler

interface TotrinnsvurderingVisitor {
    fun visitTotrinnsvurdering(
        vedtaksperiodeId: UUID,
        erRetur: Boolean,
        saksbehandler: Saksbehandler?,
        beslutter: Saksbehandler?,
        utbetalingId: UUID?,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime?
    ) {}
}