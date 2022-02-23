package no.nav.helse.modell.automatisering

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.automatiskAvvistÅrsakerTeller
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.HentEnhetløsning
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.Utbetalingsfilter
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory
import java.util.*

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

        if (!erEgenAnsatt && !tilhørerEnhetUtland && !underVergemål && utbetalingsfilter.kanUtbetales) {
            if (utbetalingsfilter.plukketUtForUtbetalingTilSykmeldt) sikkerLogg("Plukket ut for utbetaling til sykmeldt")
            return true
        }

        val årsaker = mutableListOf<String>()
        if (erEgenAnsatt) årsaker.add("Egen ansatt")
            .also { automatiskAvvistÅrsakerTeller.labels("Egen ansatt").inc() }
        if (tilhørerEnhetUtland) årsaker.add("Utland")
            .also { automatiskAvvistÅrsakerTeller.labels("Utland").inc() }
        if (underVergemål) årsaker.add("Vergemål")
            .also { automatiskAvvistÅrsakerTeller.labels("Vergemål").inc() }
        if (utbetalingsfilter.kanIkkeUtbetales) { årsaker.addAll(utbetalingsfilter.årsaker())
            .also { utbetalingsfilter.årsaker().forEach { automatiskAvvistÅrsakerTeller.labels(it).inc() } }
        }

        val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
        godkjenningMediator.automatiskAvvisning(context, behov, vedtaksperiodeId, fødselsnummer, årsaker.toList(), hendelseId)
        sikkerLogg("Automatisk avvisning av vedtaksperiode pga:$årsaker")
        return ferdigstill(context)
    }

    private fun sikkerLogg(melding: String) = sikkerLogg.info(
        melding,
        keyValue("vedtaksperiodeId", "$vedtaksperiodeId"),
        keyValue("fødselsnummer", fødselsnummer),
        keyValue("hendelseId", "$hendelseId")
    )

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
