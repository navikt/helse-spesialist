package no.nav.helse.modell.automatisering

import no.nav.helse.avvistPåGrunnAvEgenAnsattTeller
import no.nav.helse.avvistPåGrunnAvUtlandTeller
import no.nav.helse.avvistPåGrunnAvVergemålTeller
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.HentEnhetløsning
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory
import java.util.*

internal class AutomatiskAvvisningCommand(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val egenAnsattDao: EgenAnsattDao,
    val personDao: PersonDao,
    val vergemålDao: VergemålDao,
    val godkjenningsbehovJson: String,
    val godkjenningMediator: GodkjenningMediator,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        val erEgenAnsatt = egenAnsattDao.erEgenAnsatt(fødselsnummer) ?: false
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val underVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false

        if (!erEgenAnsatt && !tilhørerEnhetUtland && !underVergemål) return true

        val årsaker = mutableListOf<String>()
        if (erEgenAnsatt) årsaker.add("Egen ansatt")
            .also { avvistPåGrunnAvEgenAnsattTeller.inc() }
        if (tilhørerEnhetUtland) årsaker.add("Utland")
            .also { avvistPåGrunnAvUtlandTeller.inc() }
        if (underVergemål) årsaker.add("Vergemål")
            .also { avvistPåGrunnAvVergemålTeller.inc() }

        val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
        godkjenningMediator.automatiskAvvisning(context, behov, vedtaksperiodeId, fødselsnummer, årsaker.toList())
        logg.info("Automatisk avvisning for vedtaksperiode:$vedtaksperiodeId pga:$årsaker")
        sikkerLogg.info("Automatisk avvisning for vedtaksperiode:$vedtaksperiodeId pga:$årsaker")
        return ferdigstill(context)
    }

    private companion object {
        val logg = LoggerFactory.getLogger(this::class.java)
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
