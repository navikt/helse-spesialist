package no.nav.helse.spesialist.domain

import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.domain.ddd.Entity
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class VarselId(
    val value: UUID,
)

class Varsel private constructor(
    id: VarselId,
    val spleisBehandlingId: SpleisBehandlingId,
    status: Status,
    vurdering: Varselvurdering?,
    val kode: String,
) : Entity<VarselId>(id) {
    var status: Status = status
        private set
    var vurdering: Varselvurdering? = vurdering
        private set

    enum class Status {
        AKTIV,
        INAKTIV,
        GODKJENT,
        VURDERT,
        AVVIST,
        AVVIKLET,
    }

    fun kanGodkjennes() = status == Status.VURDERT

    fun manglerVurdering() = status == Status.AKTIV

    fun godkjenn(saksbehandlerId: SaksbehandlerOid) {
        if (!kanGodkjennes()) error("Kan ikke godkjennes, varselet er ikke vurdert")
        status = Status.GODKJENT
        vurdering =
            Varselvurdering(
                saksbehandlerId = saksbehandlerId,
                tidspunkt = LocalDateTime.now(),
            )
    }

    companion object {
        fun fraLagring(
            id: VarselId,
            spleisBehandlingId: SpleisBehandlingId,
            status: Status,
            vurdering: Varselvurdering?,
            kode: String,
        ): Varsel =
            Varsel(
                id = id,
                spleisBehandlingId = spleisBehandlingId,
                status = status,
                vurdering = vurdering,
                kode = kode,
            )
    }
}
