package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.Entity
import java.util.UUID

@JvmInline
value class VedtaksperiodeId(
    val value: UUID,
)

class Vedtaksperiode(
    id: VedtaksperiodeId,
    val fødselsnummer: String,
) : Entity<VedtaksperiodeId>(id)
