package no.nav.helse.db

import java.util.UUID

interface AvviksvurderingRepository {
    fun opprettKobling(
        avviksvurderingId: UUID,
        vilkårsgrunnlagId: UUID,
    )
}
