package no.nav.helse.modell.tildeling

import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

internal class TildelingService(
    private val saksbehandlerDao: SaksbehandlerDao,
    private val tildelingDao: TildelingDao,
    private val hendelseMediator: HendelseMediator,
    private val riskSaksbehandlergruppe: UUID,
    private val kode7Saksbehandlergruppe: UUID,
    private val beslutterSaksbehandlergruppe: UUID,
    private val skjermedePersonerSaksbehandlergruppe: UUID
) {

    internal fun tildelOppgaveTilSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
        epostadresse: String,
        navn: String,
        ident: String,
        gruppetilganger: List<UUID>
    ) {
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerreferanse, navn, epostadresse, ident)
        tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandlerreferanse, gruppetilganger)
    }

    private fun tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId: Long, saksbehandlerreferanse: UUID, gruppetilganger: List<UUID>) {
        kanTildele(oppgaveId, saksbehandlerreferanse, gruppetilganger)
        val suksess = hendelseMediator.tildelOppgaveTilSaksbehandler(oppgaveId, saksbehandlerreferanse)
        if (!suksess) {
            val eksisterendeTildeling = tildelingDao.tildelingForOppgave(oppgaveId)
            throw eksisterendeTildeling?.let { OppgaveAlleredeTildelt(eksisterendeTildeling) }
                ?: RuntimeException("Oppgave allerede tildelt, deretter feil ved les av saksbehandlernavn")
        }
    }

    private fun kanTildele(oppgaveId: Long, saksbehandlerreferanse: UUID, gruppetilganger: List<UUID>)  {
        if ("dev-gcp" != System.getenv("NAIS_CLUSTER_NAME") && hendelseMediator.erBeslutteroppgave(oppgaveId)) {
            check(!hendelseMediator.erTidligereSaksbehandler(oppgaveId, saksbehandlerreferanse)) {
                "Oppgave er beslutteroppgave, og kan ikke attesteres av samme saksbehandler som sendte til godkjenning"
            }
            check(sakbehandlertilganger(gruppetilganger).harTilgangTilBeslutterOppgaver()) {
                "Saksbehandler har ikke beslutter-tilgang"
            }
        }
    }

    internal fun fjernTildelingOgTildelNySaksbehandlerHvisFinnes(oppgaveId: Long, saksbehandlerOid: UUID?, gruppetilganger: List<UUID>) {
        fjernTildeling(oppgaveId)
        if (saksbehandlerOid != null) {
            sikkerLog.info("Fjerner gammel tildeling og tildeler oppgave $oppgaveId til saksbehandler $saksbehandlerOid")
            tildelOppgaveTilEksisterendeSaksbehandler(oppgaveId, saksbehandlerOid, gruppetilganger)
        }
    }

    internal fun fjernTildeling(oppgaveId: Long) = tildelingDao.slettTildeling(oppgaveId)

    private fun sakbehandlertilganger(gruppetilganger: List<UUID>) =
        SaksbehandlerTilganger(
            gruppetilganger = gruppetilganger,
            kode7Saksbehandlergruppe = kode7Saksbehandlergruppe,
            riskSaksbehandlergruppe = riskSaksbehandlergruppe,
            beslutterSaksbehandlergruppe = beslutterSaksbehandlergruppe,
            skjermedePersonerSaksbehandlergruppe = skjermedePersonerSaksbehandlergruppe,
        )
}
