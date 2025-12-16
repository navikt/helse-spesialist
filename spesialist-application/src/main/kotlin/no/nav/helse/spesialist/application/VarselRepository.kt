package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId

interface VarselRepository {
    fun finn(varselId: VarselId): Varsel?

    fun finnVarsler(behandlingIder: List<SpleisBehandlingId>): List<Varsel>

    fun finnVarslerFor(behandlingUnikId: BehandlingUnikId): List<Varsel>

    fun finnVarslerFor(behandlingUnikIder: List<BehandlingUnikId>): List<Varsel>

    fun lagre(varsel: Varsel)

    fun lagre(varsler: List<Varsel>)
}
