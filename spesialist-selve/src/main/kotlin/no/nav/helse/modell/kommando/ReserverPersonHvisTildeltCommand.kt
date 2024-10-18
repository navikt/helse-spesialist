package no.nav.helse.modell.kommando

import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.ReservasjonRepository
import no.nav.helse.db.TildelingRepository
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class ReserverPersonHvisTildeltCommand(
    private val fødselsnummer: String,
    private val reservasjonRepository: ReservasjonRepository,
    private val tildelingRepository: TildelingRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator,
) : Command {
    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val tildeltSaksbehandler = tildelingRepository.tildelingForPerson(fødselsnummer) ?: return true
        val vedtaksperiodeId = oppgaveRepository.finnVedtaksperiodeId(fødselsnummer)
        val totrinnsvurdering = totrinnsvurderingMediator.hentAktiv(vedtaksperiodeId)
        val saksbehandlerOid: UUID =
            if (totrinnsvurdering?.erBeslutteroppgave() == true) {
                totrinnsvurdering.saksbehandler ?: tildeltSaksbehandler.oid
            } else {
                tildeltSaksbehandler.oid
            }

        sikkerLogg.info("Oppretter reservasjon for $fødselsnummer til $saksbehandlerOid pga eksisterende tildeling")
        reservasjonRepository.reserverPerson(saksbehandlerOid, fødselsnummer)

        return true
    }
}
