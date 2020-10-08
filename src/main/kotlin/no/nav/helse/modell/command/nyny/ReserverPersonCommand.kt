package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.tildeling.ReservasjonDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class ReserverPersonCommand(
    private val oid: UUID,
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao
): Command {
    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }
    override fun execute(context: CommandContext): Boolean {
        sikkerLogg.info("reserverer $fødselsnummer til saksbehandler med oid $oid")
        reservasjonDao.reserverPerson(oid, fødselsnummer)
        return true
    }
}
