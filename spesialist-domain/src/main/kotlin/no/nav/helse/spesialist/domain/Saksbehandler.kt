package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.util.UUID

@JvmInline
value class SaksbehandlerOid(
    val value: UUID,
) : ValueObject

class Saksbehandler(
    id: SaksbehandlerOid,
    val navn: String,
    val epost: String,
    val ident: NAVIdent,
) : AggregateRoot<SaksbehandlerOid>(id) {
    override fun toString(): String = "epostadresse=$epost, oid=${id.value}"
}
