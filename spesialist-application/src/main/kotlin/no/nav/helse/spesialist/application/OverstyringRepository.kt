package no.nav.helse.spesialist.application

import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId

interface OverstyringRepository {
    fun lagre(
        overstyringer: List<Overstyring>,
        totrinnsvurderingId: TotrinnsvurderingId,
    )

    fun finnAktive(totrinnsvurderingId: TotrinnsvurderingId): List<Overstyring>
}
