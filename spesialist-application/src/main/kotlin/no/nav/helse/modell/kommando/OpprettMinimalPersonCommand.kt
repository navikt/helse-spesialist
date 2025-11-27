package no.nav.helse.modell.kommando

import no.nav.helse.db.PersonDao
import no.nav.helse.spesialist.application.logg.logg

data class MinimalPersonDto(
    val fødselsnummer: String,
    val aktørId: String,
)

internal class OpprettMinimalPersonCommand(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val personDao: PersonDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        if (personDao.finnMinimalPerson(fødselsnummer) != null) return ignorer("Person finnes fra før")
        personDao.lagreMinimalPerson(MinimalPersonDto(fødselsnummer, aktørId))
        return true
    }

    private fun ignorer(logmsg: String): Boolean {
        logg.info(logmsg)
        return true
    }
}
