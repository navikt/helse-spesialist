package no.nav.helse.modell.varsel

import java.util.UUID
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import org.slf4j.LoggerFactory

internal class VurderEnhetUtland(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val personDao: PersonDao,
    private val hendelseId: UUID,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        if (tilhørerEnhetUtland) {
            logg.info("Håndterer varsel om utland på vedtaksperiode $vedtaksperiodeId")
            sykefraværstilfelle.håndter(Varselkode.SB_EX_5.nyttVarsel(vedtaksperiodeId), hendelseId)
        }

        return true
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(VurderEnhetUtland::class.java)
    }
}
