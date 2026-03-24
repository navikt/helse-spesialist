package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.LateIdAggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.time.LocalDate

@JvmInline
value class PåVentId(
    val value: Int,
) : ValueObject

class PåVent private constructor(
    id: PåVentId?,
    val vedtaksperiodeId: VedtaksperiodeId,
    val saksbehandlerOid: SaksbehandlerOid,
    frist: LocalDate,
    val opprettetTidspunkt: Instant,
    val dialogRef: DialogId?,
    årsaker: List<String>,
    notattekst: String?,
) : LateIdAggregateRoot<PåVentId>(id) {
    var frist: LocalDate = frist
        private set

    var årsaker: List<String> = årsaker
        private set

    var notattekst: String? = notattekst
        private set

    fun nyFrist(frist: LocalDate) {
        this.frist = frist
    }

    fun nyeÅrsaker(årsaker: List<String>) {
        this.årsaker = årsaker
    }

    fun nyNotattekst(notattekst: String?) {
        this.notattekst = notattekst
    }

    object Factory {
        fun ny(
            vedtaksperiodeId: VedtaksperiodeId,
            saksbehandlerOid: SaksbehandlerOid,
            frist: LocalDate,
            dialogRef: DialogId?,
            årsaker: List<String>,
            notattekst: String?,
        ) = PåVent(
            id = null,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
            frist = frist,
            opprettetTidspunkt = Instant.now(),
            dialogRef = dialogRef,
            årsaker = årsaker,
            notattekst = notattekst,
        )

        fun fraLagring(
            id: PåVentId,
            vedtaksperiodeId: VedtaksperiodeId,
            saksbehandlerOid: SaksbehandlerOid,
            frist: LocalDate,
            opprettetTidspunkt: Instant,
            dialogRef: DialogId?,
            årsaker: List<String>,
            notattekst: String?,
        ) = PåVent(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
            frist = frist,
            opprettetTidspunkt = opprettetTidspunkt,
            dialogRef = dialogRef,
            årsaker = årsaker,
            notattekst = notattekst,
        )
    }
}
