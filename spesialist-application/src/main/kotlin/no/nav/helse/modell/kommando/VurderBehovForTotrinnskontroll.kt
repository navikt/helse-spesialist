package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER

internal class VurderBehovForTotrinnskontroll(
    private val fødselsnummer: String,
    private val oppgaveService: OppgaveService,
    private val spleisBehandlingId: SpleisBehandlingId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val behandling = sessionContext.behandlingRepository.finn(spleisBehandlingId)
        val behandlingspakke = sessionContext.behandlingRepository.finnBehandlingspakke(behandling, fødselsnummer)
        val varslerForBehandlingspakke = sessionContext.varselRepository.finnAktiveVarslerFor(behandlingspakke)
        val varslerForBehandling = sessionContext.varselRepository.finnAktiveVarslerFor(listOf(behandling))
        val kreverTotrinnsvurdering =
            varslerForBehandlingspakke.any { it.erVarselOmMedlemskap() } || varslerForBehandling.any { it.erVarselOmManglendeInntektsmelding() }
        val vedtaksperiodeHarFerdigstiltOppgave = oppgaveService.harFerdigstiltOppgave(behandling.vedtaksperiodeId.value)

        val eksisterendeTotrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)

        if ((kreverTotrinnsvurdering && !vedtaksperiodeHarFerdigstiltOppgave) || eksisterendeTotrinnsvurdering != null) {
            logg.info("Vedtaksperioden: ${behandling.vedtaksperiodeId.value} trenger totrinnsvurdering")

            val totrinnsvurdering = eksisterendeTotrinnsvurdering ?: Totrinnsvurdering.ny(fødselsnummer)
            if (totrinnsvurdering.tilstand == AVVENTER_BESLUTTER) {
                totrinnsvurdering.settAvventerSaksbehandler()
                sessionContext.periodehistorikkDao.lagre(
                    Historikkinnslag.totrinnsvurderingAutomatiskRetur(),
                    behandling.id.value,
                )
            }

            sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)

            totrinnsvurdering.saksbehandler?.value?.let {
                oppgaveService.reserverOppgave(
                    saksbehandleroid = it,
                    fødselsnummer = fødselsnummer,
                )
            }
        }

        return true
    }
}
