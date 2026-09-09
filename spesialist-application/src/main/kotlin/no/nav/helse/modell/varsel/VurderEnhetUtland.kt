package no.nav.helse.modell.varsel

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
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
            logg.info("Håndterer varsel om utland på vedtaksperiode ${behandling.vedtaksperiodeId.value}")
            val varsel = Varsel.nytt(behandling.id, spleisBehandlingId, Varselkode.SB_EX_5.name)
            sessionContext.varselRepository.lagre(varsel)
        }

        return true
    }
}
