package no.nav.helse.modell.kommando

import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.LegacyVedtaksperiode
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingTilstand.AVVENTER_BESLUTTER
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import org.slf4j.LoggerFactory

internal class VurderBehovForTotrinnskontroll(
    private val fødselsnummer: String,
    private val vedtaksperiode: LegacyVedtaksperiode,
    private val oppgaveService: OppgaveService,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val totrinnsvurderingRepository: TotrinnsvurderingRepository,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(VurderBehovForTotrinnskontroll::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        val vedtaksperiodeId = vedtaksperiode.vedtaksperiodeId()
        val kreverTotrinnsvurdering =
            sykefraværstilfelle.harMedlemskapsvarsel(vedtaksperiodeId) ||
                sykefraværstilfelle.manglerInntektsmelding(vedtaksperiodeId)
        val vedtaksperiodeHarFerdigstiltOppgave = oppgaveService.harFerdigstiltOppgave(vedtaksperiodeId)

        val eksisterendeTotrinnsvurdering = totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)

        if ((kreverTotrinnsvurdering && !vedtaksperiodeHarFerdigstiltOppgave) || eksisterendeTotrinnsvurdering != null) {
            logg.info("Vedtaksperioden: $vedtaksperiodeId trenger totrinnsvurdering")

            val totrinnsvurdering = eksisterendeTotrinnsvurdering ?: Totrinnsvurdering.ny(fødselsnummer)
            if (totrinnsvurdering.tilstand == AVVENTER_BESLUTTER) {
                totrinnsvurdering.settAvventerSaksbehandler()
                periodehistorikkDao.lagre(
                    Historikkinnslag.totrinnsvurderingAutomatiskRetur(),
                    vedtaksperiode.gjeldendeUnikId,
                )
            }

            totrinnsvurderingRepository.lagre(totrinnsvurdering)

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
