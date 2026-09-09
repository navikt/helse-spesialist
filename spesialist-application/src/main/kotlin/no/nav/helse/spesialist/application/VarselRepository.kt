package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.Varseldefinisjon

interface VarselRepository {
    fun eksisterer(varselId: VarselId): Boolean

    fun finnOrNull(varselId: VarselId): Varsel?

    fun finnVarsler(behandlingIder: List<SpleisBehandlingId>): List<Varsel>

    fun finnVarslerFor(behandlingUnikId: BehandlingUnikId): List<Varsel>

    fun finnVarslerFor(behandlingUnikIder: List<BehandlingUnikId>): List<Varsel>

    fun finnAktiveVarslerFor(behandlinger: Collection<Behandling>): List<Varsel> = finnVarslerFor(behandlinger.map { it.id }).filter { it.status == Varsel.Status.AKTIV }

    fun lagre(varsel: Varsel)

    fun lagre(varsler: List<Varsel>)

    fun slett(varselId: VarselId)

    fun avvikle(varseldefinisjon: Varseldefinisjon)
}
