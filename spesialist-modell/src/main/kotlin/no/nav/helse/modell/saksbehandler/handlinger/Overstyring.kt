package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.modell.ddd.Entity
import java.util.UUID

abstract class Overstyring(id: Long?) : Personhandling, Entity<Long>(id) {
    abstract val saksbehandler: Saksbehandler
    abstract val eksternHendelseId: UUID
    abstract val aktørId: String
    abstract val vedtaksperiodeId: UUID
}
