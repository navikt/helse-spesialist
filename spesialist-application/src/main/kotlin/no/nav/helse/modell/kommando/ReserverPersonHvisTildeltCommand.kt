package no.nav.helse.modell.kommando

import no.nav.helse.FeatureToggles
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class ReserverPersonHvisTildeltCommand(
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao,
    private val tildelingDao: TildelingDao,
    private val oppgaveDao: OppgaveDao,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val featureToggles: FeatureToggles,
) : Command {
    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val tildeltSaksbehandler = tildelingDao.tildelingForPerson(fødselsnummer) ?: return true
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(fødselsnummer)
        val totrinnsvurdering =
            if (featureToggles.skalBenytteNyTotrinnsvurderingsløsning()) {
                totrinnsvurderingRepository.finn(fødselsnummer)
            } else {
                totrinnsvurderingRepository.finn(vedtaksperiodeId)
            }
        val saksbehandlerOid: UUID =
            if (totrinnsvurdering?.erBeslutteroppgave == true) {
                totrinnsvurdering.saksbehandler?.value ?: tildeltSaksbehandler.oid
            } else {
                tildeltSaksbehandler.oid
            }

        sikkerLogg.info("Oppretter reservasjon for $fødselsnummer til $saksbehandlerOid pga eksisterende tildeling")
        reservasjonDao.reserverPerson(saksbehandlerOid, fødselsnummer)

        return true
    }
}
