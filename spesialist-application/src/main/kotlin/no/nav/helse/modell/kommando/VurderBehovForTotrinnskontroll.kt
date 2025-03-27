package no.nav.helse.modell.kommando

import no.nav.helse.FeatureToggles
import no.nav.helse.db.OverstyringDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.spesialist.application.OverstyringRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VurderBehovForTotrinnskontroll(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val vedtaksperiode: Vedtaksperiode,
    private val oppgaveService: OppgaveService,
    private val overstyringDao: OverstyringDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val overstyringRepository: OverstyringRepository,
    private val sykefraværstilfelle: Sykefraværstilfelle,
    private val featureToggles: FeatureToggles,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(VurderBehovForTotrinnskontroll::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId()
        val kreverTotrinnsvurdering = sykefraværstilfelle.harMedlemskapsvarsel(vedtaksperiodeId)
        val vedtaksperiodeHarFerdigstiltOppgave = oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId)

        if (featureToggles.skalBenytteNyTotrinnsvurderingsløsning()) {
            nyLøype(kreverTotrinnsvurdering, vedtaksperiodeHarFerdigstiltOppgave)
        } else {
            gammelLøype(kreverTotrinnsvurdering, vedtaksperiodeHarFerdigstiltOppgave)
        }

        return true
    }

    private fun nyLøype(
        kreverTotrinnsvurdering: Boolean,
        vedtaksperiodeHarFerdigstiltOppgave: Boolean,
    ) {
        val overstyringer: List<Overstyring> = overstyringRepository.finnAktive(fødselsnummer)
        if ((kreverTotrinnsvurdering && !vedtaksperiodeHarFerdigstiltOppgave) || overstyringer.isNotEmpty()) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")

            val totrinnsvurdering =
                totrinnsvurderingRepository.finn(fødselsnummer) ?: Totrinnsvurdering.ny(vedtaksperiodeId, fødselsnummer)
            if (totrinnsvurdering.tilstand == AVVENTER_BESLUTTER) {
                totrinnsvurdering.settAvventerSaksbehandler()
                periodehistorikkDao.lagre(
                    Historikkinnslag.totrinnsvurderingAutomatiskRetur(),
                    vedtaksperiode.gjeldendeUnikId,
                )
            }
            overstyringer.forEach { overstyring ->
                if (overstyring !in totrinnsvurdering.overstyringer) {
                    totrinnsvurdering.nyOverstyring(overstyring)
                }
            }
            totrinnsvurderingRepository.lagre(totrinnsvurdering)

            totrinnsvurdering.saksbehandler?.value?.let {
                oppgaveService.reserverOppgave(
                    saksbehandleroid = it,
                    fødselsnummer = fødselsnummer,
                )
            }
        }
    }

    private fun gammelLøype(
        kreverTotrinnsvurdering: Boolean,
        vedtaksperiodeHarFerdigstiltOppgave: Boolean,
    ) {
        if ((kreverTotrinnsvurdering && !vedtaksperiodeHarFerdigstiltOppgave) ||
            overstyringDao.harVedtaksperiodePågåendeOverstyring(
                vedtaksperiodeId,
            )
        ) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")

            val totrinnsvurdering =
                totrinnsvurderingRepository.finn(vedtaksperiodeId) ?: Totrinnsvurdering.ny(
                    vedtaksperiodeId,
                    fødselsnummer,
                )
            if (totrinnsvurdering.tilstand == AVVENTER_BESLUTTER) {
                totrinnsvurdering.settAvventerSaksbehandler()
                periodehistorikkDao.lagre(
                    Historikkinnslag.totrinnsvurderingAutomatiskRetur(),
                    vedtaksperiode.gjeldendeUnikId,
                )
            }

            totrinnsvurderingRepository.lagre(totrinnsvurdering)

            overstyringDao.kobleOverstyringerMedTotrinnsvurdering(totrinnsvurdering.id(), vedtaksperiodeId)

            totrinnsvurdering.saksbehandler?.value?.let {
                oppgaveService.reserverOppgave(
                    saksbehandleroid = it,
                    fødselsnummer = fødselsnummer,
                )
            }
        }
    }
}
