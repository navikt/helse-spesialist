package no.nav.helse.modell.command.nyny

import no.nav.helse.tildeling.ReservasjonDao
import java.util.*

internal class ReserverPersonCommand(
    private val oid: UUID,
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao
): Command {
    override fun execute(context: CommandContext): Boolean {
        reservasjonDao.reserverPerson(oid, fødselsnummer)

        return true
    }
}
