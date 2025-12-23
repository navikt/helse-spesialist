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

sealed interface ResultatAvSletting {
    data object Slettet : ResultatAvSletting

    data object FantesIkke : ResultatAvSletting
}

class Varsel private constructor(
    id: VarselId,
    spleisBehandlingId: SpleisBehandlingId?,
    behandlingUnikId: BehandlingUnikId,
    val kode: String,
    status: Status,
    val opprettetTidspunkt: LocalDateTime,
    vurdering: Varselvurdering?,
) : AggregateRoot<VarselId>(id) {
    var spleisBehandlingId: SpleisBehandlingId? = spleisBehandlingId
        private set
    var behandlingUnikId: BehandlingUnikId = behandlingUnikId
        private set
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

    fun erVarselOmAvvik(): Boolean = this.kode == "RV_IV_2"

    fun erInaktivt(): Boolean = this.status == Status.INAKTIV

    fun kanAvvises() = status in listOf(Status.AKTIV, Status.VURDERT)

    fun kanGodkjennes() = status == Status.VURDERT

    fun kanVurderes() = status == Status.AKTIV

    fun trengerVurdering() =
        when (status) {
            Status.AKTIV, Status.GODKJENT, Status.VURDERT -> vurdering == null
            Status.INAKTIV, Status.AVVIST, Status.AVVIKLET -> false
        }

    fun deaktiver() {
        check(status == Status.AKTIV) { "Kan kun deaktivere varsler som er aktive" }
        status = Status.INAKTIV
    }

    fun reaktiver() {
        check(status == Status.INAKTIV) { "Kan kun reaktivere varsler som er inaktive" }
        vurdering = null
        status = Status.AKTIV
    }

    fun flyttTil(
        behandlingUnikId: BehandlingUnikId,
        spleisBehandlingId: SpleisBehandlingId?,
    ) {
        check(status == Status.AKTIV) { "Kan kun flytte varsler som er aktive" }
        this.behandlingUnikId = behandlingUnikId
        this.spleisBehandlingId = spleisBehandlingId
    }

    fun vurder(
        saksbehandlerOid: SaksbehandlerOid,
        definisjonId: VarseldefinisjonId,
    ) {
        if (!kanVurderes()) error("Kan ikke vurderes, varselet er ikke aktivt")
        vurdering =
            Varselvurdering(
                saksbehandlerId = saksbehandlerOid,
                tidspunkt = LocalDateTime.now(),
                vurdertDefinisjonId = definisjonId,
            )
        status = Status.VURDERT
    }

    fun slettVurdering(): ResultatAvSletting {
        if (vurdering != null) {
            if (status != Status.VURDERT) error("Kan ikke fjerne vurdering, varselet er ikke vurdert")
            vurdering = null
            status = Status.AKTIV
            return ResultatAvSletting.Slettet
        } else {
            return ResultatAvSletting.FantesIkke
        }
    }

    fun avvis() {
        if (!kanAvvises()) error("Skal ikke avvises, varselet er ikke aktivt eller vurdert")
        status = Status.AVVIST
    }

    fun godkjenn() {
        if (!kanGodkjennes()) error("Kan ikke godkjennes, varselet er ikke vurdert")
        status = Status.GODKJENT
    }

    companion object {
        fun nytt(
            id: VarselId,
            behandlingUnikId: BehandlingUnikId,
            spleisBehandlingId: SpleisBehandlingId?,
            kode: String,
            opprettetTidspunkt: LocalDateTime,
        ): Varsel =
            Varsel(
                id = id,
                spleisBehandlingId = spleisBehandlingId,
                behandlingUnikId = behandlingUnikId,
                kode = kode,
                opprettetTidspunkt = opprettetTidspunkt,
                vurdering = null,
                status = Status.AKTIV,
            )

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
