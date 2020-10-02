package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

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
        val enhet = context.get<HentEnhetLøsning>() ?: return trengerMerInformasjon(context)
        val personinfo = context.get<HentPersoninfoLøsning>() ?: return trengerMerInformasjon(context)
        val infotrygdutbetalinger = context.get<HentInfotrygdutbetalingerLøsning>() ?: return trengerMerInformasjon(context)
        return opprettPerson(enhet, personinfo, infotrygdutbetalinger)
    }

    // TODO: om CommandContext persisteres sammen med løsningene så kunne vi bare
    // ha trengt å bedt om det som faktisk manglet
    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        log.info("Trenger mer informasjon for å opprette person")
        // TODO: Behovappene bruker bare "vedtaksperiodeId" til logging
        val liksomId = UUID.randomUUID()
        context.behov("HentPersoninfo", mapOf(
            "vedtaksperiodeId" to liksomId
        ))
        context.behov("HentEnhet", mapOf(
            "vedtaksperiodeId" to liksomId
        ))
        context.behov("HentInfotrygdutbetalinger", mapOf(
            "vedtaksperiodeId" to liksomId,
            "historikkFom" to LocalDate.now().minusYears(3),
            "historikkTom" to LocalDate.now()
        ))
        return false
    }

    private fun opprettPerson(enhet: HentEnhetLøsning, personinfo: HentPersoninfoLøsning, infotrygdutbetalinger: HentInfotrygdutbetalingerLøsning): Boolean {
        log.info("Oppretter person")
        val navnId = personinfo.lagre(personDao)
        val infotrygdutbetalingerId = infotrygdutbetalinger.lagre(personDao)
        enhet.lagrePerson(personDao, fødselsnummer, aktørId, navnId, infotrygdutbetalingerId)
        return true
    }
}
