package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.overstyringer.Overstyring

interface OverstyringRepository {
    fun lagre(
        overstyringer: List<Overstyring>,
        totrinnsvurderingId: TotrinnsvurderingId,
    )

    fun finnAktive(totrinnsvurderingId: TotrinnsvurderingId): List<Overstyring>
}
