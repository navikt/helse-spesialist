package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel

interface VarselRepository {
    fun finnVarsler(behandlingIder: List<SpleisBehandlingId>): List<Varsel>

    fun finnVarslerFor(behandlingUnikIder: List<BehandlingUnikId>): List<Varsel>

    fun lagre(varsel: Varsel)

    fun lagre(varsler: List<Varsel>)
}
