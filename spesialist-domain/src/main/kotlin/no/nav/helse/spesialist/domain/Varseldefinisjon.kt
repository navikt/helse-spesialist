package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.Entity
import java.util.UUID

@JvmInline
value class VarseldefinisjonId(
    val value: UUID,
)

class Varseldefinisjon private constructor(
    id: VarseldefinisjonId,
    val kode: String,
    val tittel: String,
) : Entity<VarseldefinisjonId>(id) {
    companion object {
        fun fraLagring(
            id: VarseldefinisjonId,
            kode: String,
            tittel: String,
        ) = Varseldefinisjon(id, kode, tittel)
    }
}
