package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.LateIdAggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class NotatId(
    val value: Int,
) : ValueObject

class Notat private constructor(
    id: NotatId?,
    val type: NotatType,
    val tekst: String,
    val dialogRef: DialogId,
    val vedtaksperiodeId: UUID,
    val saksbehandlerOid: SaksbehandlerOid,
    val opprettetTidspunkt: LocalDateTime,
    feilregistrert: Boolean,
    feilregistrertTidspunkt: LocalDateTime?,
) : LateIdAggregateRoot<NotatId>(id) {
    var feilregistrert: Boolean = feilregistrert
        private set
    var feilregistrertTidspunkt: LocalDateTime? = feilregistrertTidspunkt
        private set

    fun feilregistrer() {
        if (feilregistrert) return
        feilregistrert = true
        feilregistrertTidspunkt = LocalDateTime.now()
    }

    object Factory {
        fun ny(
            type: NotatType,
            tekst: String,
            dialogRef: DialogId,
            vedtaksperiodeId: UUID,
            saksbehandlerOid: SaksbehandlerOid,
        ) = Notat(
            id = null,
            type = type,
            tekst = tekst,
            dialogRef = dialogRef,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
            opprettetTidspunkt = LocalDateTime.now(),
            feilregistrert = false,
            feilregistrertTidspunkt = null,
        )

        fun fraLagring(
            id: NotatId,
            type: NotatType,
            tekst: String,
            dialogRef: DialogId,
            vedtaksperiodeId: UUID,
            saksbehandlerOid: SaksbehandlerOid,
            opprettetTidspunkt: LocalDateTime,
            feilregistrert: Boolean,
            feilregistrertTidspunkt: LocalDateTime?,
        ) = Notat(
            id = id,
            type = type,
            tekst = tekst,
            dialogRef = dialogRef,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
            opprettetTidspunkt = opprettetTidspunkt,
            feilregistrert = feilregistrert,
            feilregistrertTidspunkt = feilregistrertTidspunkt,
        )
    }
}
