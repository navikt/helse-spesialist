package no.nav.helse.modell.kommando

import no.nav.helse.avvistPåGrunnAvUtlandTeller
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.meldinger.HentEnhetløsning
import no.nav.helse.mediator.meldinger.HentInfotrygdutbetalingerløsning
import no.nav.helse.mediator.meldinger.HentPersoninfoløsning
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class OppdaterPersonCommand(
    fødselsnummer: String,
    personDao: PersonDao,
    godkjenningsbehovJson: String,
    vedtaksperiodeId: UUID,
    godkjenningMediator: GodkjenningMediator,
) : MacroCommand() {
    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterPersonCommand::class.java)
    }

    override val commands: List<Command> = listOf(
        OppdaterPersoninfoCommand(fødselsnummer, personDao),
        OppdaterEnhetCommand(fødselsnummer, personDao, godkjenningsbehovJson, vedtaksperiodeId, godkjenningMediator),
        OppdaterInfotrygdutbetalingerCommand(fødselsnummer, personDao)
    )

    private abstract class OppdaterCommand(
        private val fødselsnummer: String,
        private val personDao: PersonDao,
        private val behov: String,
        private val parametere: Map<String, Any> = emptyMap()
    ) : Command {
        override fun execute(context: CommandContext): Boolean {
            if (erOppdatert(personDao, fødselsnummer)) return ignorer()
            return behandle(context, personDao, fødselsnummer)
        }

        override fun resume(context: CommandContext): Boolean {
            return behandle(context, personDao, fødselsnummer)
        }

        private fun ignorer(): Boolean {
            log.info("har ikke behov for $behov, informasjonen er ny nok")
            return true
        }

        protected abstract fun erOppdatert(personDao: PersonDao, fødselsnummer: String): Boolean

        protected abstract fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean

        protected fun trengerMerInformasjon(context: CommandContext): Boolean {
            log.info("trenger oppdatert $behov")
            context.behov(behov, parametere)
            return false
        }
    }

    private class OppdaterPersoninfoCommand(fødselsnummer: String, personDao: PersonDao) : OppdaterCommand(fødselsnummer, personDao, "HentPersoninfo") {
        override fun erOppdatert(personDao: PersonDao, fødselsnummer: String): Boolean {
            val sistOppdatert = personDao.findPersoninfoSistOppdatert(fødselsnummer)
            return sistOppdatert > LocalDate.now().minusDays(14)
        }

        override fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean {
            val personinfo = context.get<HentPersoninfoløsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer personinfo")
            personinfo.oppdater(personDao, fødselsnummer)
            return true
        }
    }

    private class OppdaterEnhetCommand(
        fødselsnummer: String,
        personDao: PersonDao,
        private val godkjenningsbehovJson: String,
        private val vedtaksperiodeId: UUID,
        private val godkjenningMediator: GodkjenningMediator,
    ) : OppdaterCommand(fødselsnummer, personDao, "HentEnhet") {
        override fun erOppdatert(personDao: PersonDao, fødselsnummer: String): Boolean {
            val sistOppdatert = personDao.findEnhetSistOppdatert(fødselsnummer)
            return sistOppdatert > LocalDate.now().minusDays(5)
        }

        override fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean {
            val enhet = context.get<HentEnhetløsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer enhetsnr")
            if (enhet.tilhørerUtlandEnhet()) {
                val behov = UtbetalingsgodkjenningMessage(godkjenningsbehovJson)
                behov.avvisAutomatisk(listOf("Utland"))
                context.publiser(behov.toJson())
                context.publiser(godkjenningMediator.lagVedtaksperiodeAvvist(vedtaksperiodeId, fødselsnummer, behov).toJson())
                avvistPåGrunnAvUtlandTeller.inc()
                log.info("Automatisk avvisning for vedtaksperiode $vedtaksperiodeId")
            }
            enhet.oppdater(personDao, fødselsnummer)
            return true
        }
    }

    private class OppdaterInfotrygdutbetalingerCommand(fødselsnummer: String, personDao: PersonDao) :
        OppdaterCommand(
            fødselsnummer = fødselsnummer,
            personDao = personDao,
            behov = "HentInfotrygdutbetalinger",
            parametere = mapOf(
                "historikkFom" to LocalDate.now().minusYears(3),
                "historikkTom" to LocalDate.now()
            )
        ) {
        override fun erOppdatert(personDao: PersonDao, fødselsnummer: String): Boolean {
            val sistOppdatert = personDao.findITUtbetalingsperioderSistOppdatert(fødselsnummer)
            return sistOppdatert > LocalDate.now().minusDays(1)
        }

        override fun behandle(context: CommandContext, personDao: PersonDao, fødselsnummer: String): Boolean {
            val utbetalinger = context.get<HentInfotrygdutbetalingerløsning>() ?: return trengerMerInformasjon(context)
            log.info("oppdaterer utbetalinger fra Infotrygd")
            utbetalinger.oppdater(personDao, fødselsnummer)
            return true
        }
    }
}
