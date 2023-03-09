package no.nav.helse.modell.automatisering

import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.løsninger.HentEnhetløsning
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.Utbetalingsfilter
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory
import java.util.UUID

internal class AutomatiskAvvisningCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val egenAnsattDao: EgenAnsattDao,
    private val personDao: PersonDao,
    private val vergemålDao: VergemålDao,
    private val godkjenningsbehovJson: String,
    private val godkjenningMediator: GodkjenningMediator,
    private val hendelseId: UUID,
    private val utbetalingsfilter: () -> Utbetalingsfilter
) : Command {

    override fun execute(context: CommandContext): Boolean {
        val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer) ?: false
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val underVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val utbetalingsfilter = utbetalingsfilter()

        if (!erEgenAnsatt && !tilhørerEnhetUtland && !underVergemål && utbetalingsfilter.kanUtbetales) return true

        val årsaker = mutableListOf<String>()
        if (erEgenAnsatt) årsaker.add("Egen ansatt")
        if (tilhørerEnhetUtland) årsaker.add("Utland")
        if (underVergemål) årsaker.add("Vergemål")
        if (utbetalingsfilter.kanIkkeUtbetales) årsaker.addAll(utbetalingsfilter.årsaker())

        val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
        godkjenningMediator.automatiskAvvisning(context, behov, vedtaksperiodeId, fødselsnummer, årsaker.toList(), hendelseId)
        logg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga:$årsaker")
        return ferdigstill(context)
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiskAvvisningCommand::class.java)
    }
}
