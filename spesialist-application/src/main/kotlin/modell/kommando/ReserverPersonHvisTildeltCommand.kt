package no.nav.helse.modell.kommando

import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import java.util.UUID

internal class ReserverPersonHvisTildeltCommand(
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao,
    private val tildelingDao: TildelingDao,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val tildeltSaksbehandler = tildelingDao.tildelingForPerson(fødselsnummer) ?: return true
        val totrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        val saksbehandlerOid: UUID =
            if (totrinnsvurdering?.tilstand == AVVENTER_BESLUTTER) {
                totrinnsvurdering.saksbehandler?.value ?: tildeltSaksbehandler.oid
            } else {
                tildeltSaksbehandler.oid
            }

        teamLogs.info("Oppretter reservasjon for $fødselsnummer til $saksbehandlerOid pga eksisterende tildeling")
        reservasjonDao.reserverPerson(saksbehandlerOid, fødselsnummer)

        return true
    }
}
