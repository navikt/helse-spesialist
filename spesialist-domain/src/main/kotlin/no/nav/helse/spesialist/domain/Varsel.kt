package no.nav.helse.spesialist.domain

import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.LocalDateTime
import java.util.UUID

@JvmInline
value class VarselId(
    val value: UUID,
) : ValueObject

class Varsel private constructor(
    id: VarselId,
    val spleisBehandlingId: SpleisBehandlingId?,
    val behandlingUnikId: BehandlingUnikId,
    val kode: String,
    status: Status,
    val opprettetTidspunkt: LocalDateTime,
    vurdering: Varselvurdering?,
) : AggregateRoot<VarselId>(id) {
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
            kode: String,
            opprettetTidspunkt: LocalDateTime,
            vurdering: Varselvurdering?,
        ): Varsel =
            Varsel(
                id = id,
                spleisBehandlingId = spleisBehandlingId,
                behandlingUnikId = behandlingUnikId,
                kode = kode,
                status = status,
                opprettetTidspunkt = opprettetTidspunkt,
                vurdering = vurdering,
            )
    }
}
