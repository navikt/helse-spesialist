package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.PåVentId
import no.nav.helse.spesialist.domain.VedtaksperiodeId

interface PåVentRepository {
    fun finnAlle(ider: Set<PåVentId>): List<PåVent>

    fun finnFor(vedtaksperiodeId: VedtaksperiodeId): PåVent?

    fun lagre(påVent: PåVent)

    fun slett(id: PåVentId)
}
