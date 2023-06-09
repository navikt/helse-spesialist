package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class ReserverPersonHvisTildeltCommand(
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao,
    private val tildelingDao: TildelingDao,
    private val oppgaveDao: OppgaveDao,
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator,
) : Command {
    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val tildeltSaksbehandler = tildelingDao.tildelingForPerson(fødselsnummer) ?: return true
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(fødselsnummer)
        val totrinnsvurdering = totrinnsvurderingMediator.hentAktiv(vedtaksperiodeId)
        val saksbehandlerOid: UUID =
            if (totrinnsvurdering?.erBeslutteroppgave() == true)
                totrinnsvurdering.saksbehandler ?: tildeltSaksbehandler.oid
            else tildeltSaksbehandler.oid
        val påVent = if (totrinnsvurdering?.erBeslutteroppgave() == true) false else tildeltSaksbehandler.påVent

        sikkerLogg.info("Oppretter reservasjon for $fødselsnummer til $saksbehandlerOid pga eksisterende tildeling")
        reservasjonDao.reserverPerson(saksbehandlerOid, fødselsnummer, påVent)

        return true
    }
}
