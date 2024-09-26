package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonRepository
import org.slf4j.LoggerFactory

data class MinimalPersonDto(
    val fødselsnummer: String,
    val aktørId: String,
)

internal class OpprettMinimalPersonCommand(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val personRepository: PersonRepository,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(OpprettMinimalPersonCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (personRepository.finnMinimalPerson(fødselsnummer) != null) return ignorer("Person finnes fra før")
        personRepository.lagreMinimalPerson(MinimalPersonDto(fødselsnummer, aktørId))
        return true
    }

    private fun ignorer(logmsg: String): Boolean {
        logg.info(logmsg)
        return true
    }
}
