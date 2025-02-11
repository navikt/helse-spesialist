package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.modell.ddd.Entity
import java.util.UUID

@JvmInline
value class OverstyringId(val value: Long)

abstract class Overstyring(id: OverstyringId?) : Personhandling, Entity<OverstyringId>(id) {
    abstract val saksbehandlerOid: UUID
    abstract val eksternHendelseId: UUID
    abstract val aktørId: String
    abstract val vedtaksperiodeId: UUID
}
