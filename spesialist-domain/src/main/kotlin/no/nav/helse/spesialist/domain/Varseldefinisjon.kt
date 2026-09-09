package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class VarseldefinisjonId(
    val value: UUID,
) : ValueObject

class Varseldefinisjon(
    id: VarseldefinisjonId,
    val kode: String,
    val tittel: String,
    val forklaring: String?,
    val handling: String?,
    val avviklet: Boolean,
    val opprettet: LocalDateTime,
) : AggregateRoot<VarseldefinisjonId>(id) {
    companion object {
        fun fraLagring(
            id: VarseldefinisjonId,
            kode: String,
            tittel: String,
            forklaring: String?,
            handling: String?,
            avviklet: Boolean,
            opprettet: LocalDateTime,
        ): Varseldefinisjon =
            Varseldefinisjon(
                id = id,
                kode = kode,
                tittel = tittel,
                forklaring = forklaring,
                handling = handling,
                avviklet = avviklet,
                opprettet = opprettet,
            )
    }
}
