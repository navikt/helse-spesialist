package no.nav.helse.modell.tildeling

import java.util.UUID
import no.nav.helse.Gruppe
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.ApiTilgangskontroll
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

internal class TildelingService(
    private val saksbehandlerDao: SaksbehandlerDao,
    private val tildelingDao: TildelingDao,
    private val hendelseMediator: HendelseMediator,
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator,
) {

    internal fun tildelOppgaveTilSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
        epostadresse: String,
        navn: String,
        ident: String,
        tilgangskontroll: ApiTilgangskontroll,
    ) {
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerreferanse, navn, epostadresse, ident)
        tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandlerreferanse, tilgangskontroll)
    }

    private fun tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId: Long, saksbehandlerreferanse: UUID, tilgangskontroll: ApiTilgangskontroll) {
        kanTildele(oppgaveId, saksbehandlerreferanse, tilgangskontroll)
        val suksess = hendelseMediator.tildelOppgaveTilSaksbehandler(oppgaveId, saksbehandlerreferanse)
        if (!suksess) {
            val eksisterendeTildeling = tildelingDao.tildelingForOppgave(oppgaveId)
            throw eksisterendeTildeling?.let { OppgaveAlleredeTildelt(eksisterendeTildeling) }
                ?: RuntimeException("Oppgave allerede tildelt, deretter feil ved les av saksbehandlernavn")
        }
    }

    private fun kanTildele(oppgaveId: Long, saksbehandlerreferanse: UUID, tilgangskontroll: ApiTilgangskontroll) {
        totrinnsvurderingMediator.hentAktiv(oppgaveId)
            ?.takeIf { "dev-gcp" != System.getenv("NAIS_CLUSTER_NAME") && it.erBeslutteroppgave() }
            ?.let { totrinnsvurdering ->
                check(totrinnsvurdering.saksbehandler == saksbehandlerreferanse) {
                    "Oppgave er beslutteroppgave, og kan ikke attesteres av samme saksbehandler som sendte til godkjenning"
                }
                check(tilgangskontroll.harTilgangTil(Gruppe.BESLUTTER)) {
                    "Saksbehandler har ikke beslutter-tilgang"
                }
            }
    }

    internal fun fjernTildelingOgTildelNySaksbehandlerHvisFinnes(oppgaveId: Long, saksbehandlerOid: UUID?, tilgangskontroll: ApiTilgangskontroll) {
        fjernTildeling(oppgaveId)
        if (saksbehandlerOid != null) {
            sikkerLog.info("Fjerner gammel tildeling og tildeler oppgave $oppgaveId til saksbehandler $saksbehandlerOid")
            tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandlerOid, tilgangskontroll)
        }
    }

    internal fun fjernTildeling(oppgaveId: Long) = tildelingDao.slettTildeling(oppgaveId)
}
