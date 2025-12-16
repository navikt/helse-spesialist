package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId

class InMemoryVarselRepository :
    AbstractInMemoryRepository<VarselId, Varsel>(),
    VarselRepository {
    override fun finnVarsler(behandlingIder: List<SpleisBehandlingId>): List<Varsel> = alle().filter { it.spleisBehandlingId in behandlingIder }

    override fun finnVarslerFor(behandlingUnikId: BehandlingUnikId): List<Varsel> = alle().filter { it.behandlingUnikId == behandlingUnikId }

    override fun finnVarslerFor(behandlingUnikIder: List<BehandlingUnikId>): List<Varsel> = alle().filter { it.behandlingUnikId in behandlingUnikIder }

    override fun lagre(varsler: List<Varsel>) {
        varsler.forEach(::lagre)
    }

    override fun deepCopy(original: Varsel): Varsel =
        Varsel.fraLagring(
            id = original.id,
            spleisBehandlingId = original.spleisBehandlingId,
            behandlingUnikId = original.behandlingUnikId,
            status = original.status,
            kode = original.kode,
            opprettetTidspunkt = original.opprettetTidspunkt,
            vurdering = original.vurdering,
        )
}
