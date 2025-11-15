package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import java.util.UUID

@JvmInline
value class VedtaksperiodeId(
    val value: UUID,
)

class Vedtaksperiode(
    id: VedtaksperiodeId,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val forkastet: Boolean,
) : AggregateRoot<VedtaksperiodeId>(id)
