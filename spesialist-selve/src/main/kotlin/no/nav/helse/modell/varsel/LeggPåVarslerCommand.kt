package no.nav.helse.modell.varsel

import java.util.UUID
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory

internal class LeggPåVarslerCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val personDao: PersonDao,
    private val vergemålDao: VergemålDao,
    private val hendelseId: UUID,
    private val utbetaling: Utbetaling,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val underVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false

        if (underVergemål && behold()) {
            logg.info("Legger på varsel om vergemål på vedtaksperiode $vedtaksperiodeId")
            sykefraværstilfelle.håndter(Varselkode.SB_EX_4.nyttVarsel(vedtaksperiodeId), hendelseId)
        }
        if (tilhørerEnhetUtland && behold()) {
            logg.info("Legger på varsel om utland på vedtaksperiode $vedtaksperiodeId")
            sykefraværstilfelle.håndter(Varselkode.SB_EX_5.nyttVarsel(vedtaksperiodeId), hendelseId)
        }

        return ferdigstill(context)
    }

    private fun behold() = Toggle.BeholdRevurderingerMedVergemålEllerUtland.enabled && utbetaling.erRevurdering()

    private companion object {
        private val logg = LoggerFactory.getLogger(LeggPåVarslerCommand::class.java)
    }
}
