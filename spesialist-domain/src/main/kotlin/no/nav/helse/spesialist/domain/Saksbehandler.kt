package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.Entity
import java.util.UUID

@JvmInline
value class SaksbehandlerOid(val value: UUID)

class Saksbehandler(
    id: SaksbehandlerOid,
    val navn: String,
    val epost: String,
    val ident: String,
) : Entity<SaksbehandlerOid>(id) {
    override fun toString(): String = "epostadresse=$epost, oid=${id().value}"
}
