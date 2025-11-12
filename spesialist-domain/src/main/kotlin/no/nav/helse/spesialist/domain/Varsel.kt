package no.nav.helse.spesialist.domain

import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.domain.ddd.Entity
import java.util.UUID

@JvmInline
value class VarselId(
    val value: UUID,
)

class Varsel private constructor(
    id: VarselId,
    val spleisBehandlingId: SpleisBehandlingId?,
    val behandlingUnikId: BehandlingUnikId,
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

    fun godkjenn() {
        if (!kanGodkjennes()) error("Kan ikke godkjennes, varselet er ikke vurdert")
        status = Status.GODKJENT
    }

    companion object {
        fun fraLagring(
            id: VarselId,
            spleisBehandlingId: SpleisBehandlingId?,
            behandlingUnikId: BehandlingUnikId,
            status: Status,
            vurdering: Varselvurdering?,
            kode: String,
        ): Varsel =
            Varsel(
                id = id,
                spleisBehandlingId = spleisBehandlingId,
                behandlingUnikId = behandlingUnikId,
                status = status,
                vurdering = vurdering,
                kode = kode,
            )
    }
}
