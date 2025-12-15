package no.nav.helse.modell.kommando

import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.application.logg.sikkerlogg

internal class ReserverPersonHvisTildeltCommand(
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao,
    private val tildelingDao: TildelingDao,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val tildeltSaksbehandler = tildelingDao.tildelingForPerson(fødselsnummer) ?: return true
        val totrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        val saksbehandlersIdent =
            if (totrinnsvurdering?.tilstand == AVVENTER_BESLUTTER) {
                totrinnsvurdering.saksbehandler ?: tildeltSaksbehandler.ident
            } else {
                tildeltSaksbehandler.ident
            }

        sikkerlogg.info("Oppretter reservasjon for $fødselsnummer til $saksbehandlersIdent pga eksisterende tildeling")
        reservasjonDao.reserverPerson(saksbehandlersIdent, fødselsnummer)

        return true
    }
}
