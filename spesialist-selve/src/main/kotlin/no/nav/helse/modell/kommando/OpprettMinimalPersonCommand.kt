package no.nav.helse.modell.kommando

import no.nav.helse.modell.person.PersonDao
import org.slf4j.LoggerFactory

internal class OpprettMinimalPersonCommand(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val personDao: PersonDao
) : Command {

    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettMinimalPersonCommand::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        if (personDao.findPersonByFødselsnummer(fødselsnummer) != null) return ignorer("Person finnes fra før")
        return behandle()
    }

    override fun resume(context: CommandContext): Boolean {
        return behandle()
    }

    private fun ignorer(logmsg: String): Boolean {
        logg.info(logmsg)
        return true
    }

    private fun behandle(): Boolean {
        sikkerLog.info("Oppretter minimal person for fødselsnummer: $fødselsnummer og aktørId: $aktørId")
        personDao.insertPerson(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId
        )
        return true
    }
}
