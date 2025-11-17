package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.ddd.LateIdEntity
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class OverstyringId(
    val value: Long,
) : ValueObject

sealed class Overstyring(
    id: OverstyringId?,
    ferdigstilt: Boolean,
) : LateIdEntity<OverstyringId>(id) {
    abstract val fødselsnummer: String
    abstract val saksbehandlerOid: SaksbehandlerOid
    abstract val eksternHendelseId: UUID
    abstract val aktørId: String
    abstract val vedtaksperiodeId: UUID
    abstract val opprettet: LocalDateTime
    var ferdigstilt = ferdigstilt
        private set

    fun ferdigstill() {
        ferdigstilt = true
    }
}
