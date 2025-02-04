package no.nav.helse.spesialist.modell

import no.nav.helse.spesialist.modell.ddd.AggregateRoot
import java.time.LocalDateTime
import java.util.UUID

class Notat private constructor(
    id: Int?,
    val type: NotatType,
    val tekst: String,
    val dialogRef: Long,
    val vedtaksperiodeId: UUID,
    val saksbehandlerOid: UUID,
    val opprettetTidspunkt: LocalDateTime,
    feilregistrert: Boolean,
    feilregistrertTidspunkt: LocalDateTime?,
) : AggregateRoot<Int>(id) {
    var feilregistrert: Boolean = feilregistrert
        private set
    var feilregistrertTidspunkt: LocalDateTime? = feilregistrertTidspunkt
        private set

    fun feilregistrer() {
        feilregistrert = true
        feilregistrertTidspunkt = LocalDateTime.now()
    }

    object Factory {
        fun ny(
            type: NotatType,
            tekst: String,
            dialogRef: Long,
            vedtaksperiodeId: UUID,
            saksbehandlerOid: UUID,
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
            id: Int,
            type: NotatType,
            tekst: String,
            dialogRef: Long,
            vedtaksperiodeId: UUID,
            saksbehandlerOid: UUID,
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
