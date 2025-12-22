package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.util.UUID

@JvmInline
value class VedtaksperiodeId(
    val value: UUID,
) : ValueObject

class Vedtaksperiode(
    id: VedtaksperiodeId,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val forkastet: Boolean,
) : AggregateRoot<VedtaksperiodeId>(id) {
    companion object {
        fun ny(
            id: VedtaksperiodeId,
            identitetsnummer: Identitetsnummer,
            organisasjonsnummer: String,
        ) = Vedtaksperiode(
            id = id,
            fødselsnummer = identitetsnummer.value,
            organisasjonsnummer = organisasjonsnummer,
            forkastet = false,
        )
    }
}
