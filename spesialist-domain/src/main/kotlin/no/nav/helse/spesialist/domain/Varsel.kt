package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.Entity
import java.util.UUID

@JvmInline
value class VarselId(
    val value: UUID,
)

class Varsel private constructor(
    id: VarselId,
    val spleisBehandlingId: SpleisBehandlingId,
    status: Status,
) : Entity<VarselId>(id) {
    var status: Status = status
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

    fun godkjenn() {
        if (!kanGodkjennes()) error("Kan ikke godkjennes, varselet er ikke vurdert")
        status = Status.GODKJENT
    }

    companion object {
        fun fraLagring(
            id: VarselId,
            spleisBehandlingId: SpleisBehandlingId,
            status: Status,
        ): Varsel =
            Varsel(
                id = id,
                spleisBehandlingId = spleisBehandlingId,
                status = status,
            )
    }
}
