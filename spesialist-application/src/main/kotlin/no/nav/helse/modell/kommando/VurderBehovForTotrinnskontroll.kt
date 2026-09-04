package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.spesialist.domain.VedtaksperiodeId

internal class VurderBehovForTotrinnskontroll(
    private val fødselsnummer: String,
    private val oppgaveService: OppgaveService,
    private val behandlingUnikId: BehandlingUnikId,
    private val vedtaksperiodeId: VedtaksperiodeId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val varsler = sessionContext.varselRepository.finnVarslerFor(behandlingUnikId)
        val kreverTotrinnsvurdering =
            varsler.any { it.erVarselOmMedlemsskap() } || varsler.any { it.erVarselOmManglendeInntektsmelding() }
        val vedtaksperiodeHarFerdigstiltOppgave = oppgaveService.harFerdigstiltOppgave(this.vedtaksperiodeId.value)

        val eksisterendeTotrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)

        if ((kreverTotrinnsvurdering && !vedtaksperiodeHarFerdigstiltOppgave) || eksisterendeTotrinnsvurdering != null) {
            logg.info("Vedtaksperioden: ${this.vedtaksperiodeId} trenger totrinnsvurdering")

            val totrinnsvurdering = eksisterendeTotrinnsvurdering ?: Totrinnsvurdering.ny(fødselsnummer)
            if (totrinnsvurdering.tilstand == AVVENTER_BESLUTTER) {
                totrinnsvurdering.settAvventerSaksbehandler()
                sessionContext.periodehistorikkDao.lagre(
                    Historikkinnslag.totrinnsvurderingAutomatiskRetur(),
                    behandlingUnikId.value,
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
