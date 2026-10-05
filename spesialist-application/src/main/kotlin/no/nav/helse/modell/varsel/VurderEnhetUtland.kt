package no.nav.helse.modell.varsel

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel

internal class VurderEnhetUtland(
    private val identitetsnummer: Identitetsnummer,
    private val spleisBehandlingId: SpleisBehandlingId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(sessionContext.personDao.finnEnhetId(identitetsnummer.value))
        if (tilhørerEnhetUtland) {
            val behandling = sessionContext.behandlingRepository.finn(spleisBehandlingId)
            val eksisterendeVarsler = sessionContext.varselRepository.finnVarslerFor(behandling.id)
            val eksisterendeVarsel = eksisterendeVarsler.find { it.erVarselOmNavUtland() }
            if (eksisterendeVarsel != null) {
                loggInfo("Varsel om utland finnes fra før av, oppretter ikke et nytt", "behandlingId" to behandling.id.value)
                return true
            }
            loggInfo("Indikasjoner på at vedkommende tilhører enhet utland, oppretter varsel", "behandlingId" to behandling.id.value)
            val varsel = Varsel.nytt(behandling.id, spleisBehandlingId, Varselkode.SB_EX_5)
            sessionContext.varselRepository.lagre(varsel)
        }

        return true
    }
}
