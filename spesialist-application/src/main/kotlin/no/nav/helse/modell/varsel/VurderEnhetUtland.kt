package no.nav.helse.modell.varsel

import no.nav.helse.db.PersonDao
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.spesialist.application.logg.logg
import java.util.UUID

internal class VurderEnhetUtland(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val personDao: PersonDao,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {
    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        if (tilhørerEnhetUtland) {
            logg.info("Håndterer varsel om utland på vedtaksperiode $vedtaksperiodeId")
            sykefraværstilfelle.håndter(Varselkode.SB_EX_5.nyttVarsel(vedtaksperiodeId))
        }

        return true
    }
}
