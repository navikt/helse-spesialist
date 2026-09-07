package no.nav.helse.modell.varsel

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import java.util.UUID

internal class VurderEnhetUtland(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        return sessionContext.legacyPersonRepository.brukPerson(fødselsnummer) {
            val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(sessionContext.personDao.finnEnhetId(fødselsnummer))
            if (tilhørerEnhetUtland) {
                val sykefraværstilfelle = this.sykefraværstilfelle(vedtaksperiodeId)
                logg.info("Håndterer varsel om utland på vedtaksperiode $vedtaksperiodeId")
                sykefraværstilfelle.håndter(Varselkode.SB_EX_5.nyttVarsel(vedtaksperiodeId))
            }

            return@brukPerson true
        }
    }
}
