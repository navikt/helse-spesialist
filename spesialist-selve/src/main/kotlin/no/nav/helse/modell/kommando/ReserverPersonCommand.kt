package no.nav.helse.modell.kommando

import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao

internal class ReserverPersonCommand(
    private val oid: UUID,
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao,
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
): Command {
    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }
    override fun execute(context: CommandContext): Boolean {
        sikkerLogg.info("reserverer $fødselsnummer til saksbehandler med oid $oid")
        val påVent = oppgaveDao.finnOppgaveId(fødselsnummer)?.let {
            tildelingDao.tildelingForOppgave(it)?.påVent
        } ?: false
        reservasjonDao.reserverPerson(oid, fødselsnummer, påVent)
        return true
    }
}
