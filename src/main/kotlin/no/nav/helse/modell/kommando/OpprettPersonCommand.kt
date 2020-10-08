package no.nav.helse.modell.kommando

import no.nav.helse.mediator.meldinger.HentEnhetløsning
import no.nav.helse.mediator.meldinger.HentInfotrygdutbetalingerløsning
import no.nav.helse.mediator.meldinger.HentPersoninfoløsning
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class OpprettPersonCommand(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val personDao: PersonDao
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OpprettPersonCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (personDao.findPersonByFødselsnummer(fødselsnummer) != null) return ignorer()
        return behandle(context)
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle(context)
    }

    private fun ignorer(): Boolean {
        log.info("Person finnes fra før")
        return true
    }

    private fun behandle(context: CommandContext): Boolean {
        val enhet = context.get<HentEnhetløsning>() ?: return trengerMerInformasjon(context)
        val personinfo = context.get<HentPersoninfoløsning>() ?: return trengerMerInformasjon(context)
        val infotrygdutbetalinger = context.get<HentInfotrygdutbetalingerløsning>() ?: return trengerMerInformasjon(context)
        return opprettPerson(enhet, personinfo, infotrygdutbetalinger)
    }

    // TODO: om CommandContext persisteres sammen med løsningene så kunne vi bare
    // ha trengt å bedt om det som faktisk manglet
    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        log.info("Trenger mer informasjon for å opprette person")
        context.behov("HentPersoninfo")
        context.behov("HentEnhet")
        context.behov("HentInfotrygdutbetalinger", mapOf(
            "historikkFom" to LocalDate.now().minusYears(3),
            "historikkTom" to LocalDate.now()
        ))
        return false
    }

    private fun opprettPerson(enhet: HentEnhetløsning, personinfo: HentPersoninfoløsning, infotrygdutbetalinger: HentInfotrygdutbetalingerløsning): Boolean {
        log.info("Oppretter person")
        val navnId = personinfo.lagre(personDao)
        val infotrygdutbetalingerId = infotrygdutbetalinger.lagre(personDao)
        enhet.lagrePerson(personDao, fødselsnummer, aktørId, navnId, infotrygdutbetalingerId)
        return true
    }
}
