package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import java.util.UUID

class InMemoryVarselRepository : VarselRepository, AbstractInMemoryRepository<VarselId, Varsel>() {
    override fun finnVarsler(behandlingIder: List<SpleisBehandlingId>): List<Varsel> =
        alle().filter { it.spleisBehandlingId in behandlingIder }

    override fun finnVarslerFor(behandlingUnikIder: List<BehandlingUnikId>): List<Varsel> =
        alle().filter { it.behandlingUnikId in behandlingUnikIder }

    override fun lagre(varsler: List<Varsel>) {
        varsler.forEach(::lagre)
    }

    override fun generateId(): VarselId = VarselId(UUID.randomUUID())
}
