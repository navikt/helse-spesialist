package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.spesialist.domain.VedtaksperiodeId

internal class VurderBehovForTotrinnskontroll(
    private val fødselsnummer: String,
    private val oppgaveService: OppgaveService,
    private val vedtaksperiodeId: VedtaksperiodeId,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        return sessionContext.legacyPersonRepository.brukPerson(fødselsnummer) {
            val sykefraværstilfelle = this.sykefraværstilfelle(vedtaksperiodeId.value)
            val kreverTotrinnsvurdering =
                sykefraværstilfelle.harMedlemskapsvarsel(vedtaksperiodeId.value) ||
                    sykefraværstilfelle.manglerInntektsmelding(vedtaksperiodeId.value)
            val vedtaksperiodeHarFerdigstiltOppgave = oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId.value)

            val eksisterendeTotrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)

            if ((kreverTotrinnsvurdering && !vedtaksperiodeHarFerdigstiltOppgave) || eksisterendeTotrinnsvurdering != null) {
                logg.info("Vedtaksperioden: ${vedtaksperiodeId.value} trenger totrinnsvurdering")

                val totrinnsvurdering = eksisterendeTotrinnsvurdering ?: Totrinnsvurdering.ny(fødselsnummer)
                if (totrinnsvurdering.tilstand == AVVENTER_BESLUTTER) {
                    val vedtaksperiode = this.vedtaksperiode(vedtaksperiodeId.value)
                    totrinnsvurdering.settAvventerSaksbehandler()
                    sessionContext.periodehistorikkDao.lagre(
                        Historikkinnslag.totrinnsvurderingAutomatiskRetur(),
                        vedtaksperiode.gjeldendeUnikId,
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

            return@brukPerson true
        }
    }
}
