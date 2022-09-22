package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class ReserverPersonHvisTildeltCommand(
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao,
    private val tildelingDao: TildelingDao,
    private val oppgaveDao: OppgaveDao,
) : Command {
    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val tildeltSaksbehandler = tildelingDao.tildelingForPerson(fødselsnummer) ?: return true
        val erBeslutteroppgave = oppgaveDao.erBeslutteroppgave(vedtaksperiodeId)
        val saksbehandlerOid: UUID =
            if (erBeslutteroppgave) oppgaveDao.finnOppgaveId(vedtaksperiodeId)?.let {
                oppgaveDao.finnTidligereSaksbehandler(it)
            } ?: tildeltSaksbehandler.oid
            else tildeltSaksbehandler.oid
        val påVent = if(erBeslutteroppgave) false else tildeltSaksbehandler.påVent

        sikkerLogg.info("Oppretter reservasjon for $fødselsnummer til $saksbehandlerOid pga eksisterende tildeling")
        reservasjonDao.reserverPerson(saksbehandlerOid, fødselsnummer, påVent)

        return true
    }
}
