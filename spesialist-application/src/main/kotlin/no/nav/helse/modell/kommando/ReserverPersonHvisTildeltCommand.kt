package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import java.util.UUID

internal class ReserverPersonHvisTildeltCommand(
    private val identitetsnummer: Identitetsnummer,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val tildeltSaksbehandler = sessionContext.tildelingDao.tildelingForPerson(identitetsnummer.value) ?: return true
        val totrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPersonOrNull(identitetsnummer.value)
        val saksbehandlerOid: UUID =
            if (totrinnsvurdering?.tilstand == AVVENTER_BESLUTTER) {
                totrinnsvurdering.saksbehandler?.value ?: tildeltSaksbehandler.oid
            } else {
                tildeltSaksbehandler.oid
            }

        teamLogs.info("Oppretter reservasjon for ${identitetsnummer.value} til $saksbehandlerOid pga eksisterende tildeling")
        sessionContext.reservasjonDao.reserverPerson(saksbehandlerOid, identitetsnummer.value)

        return true
    }
}
