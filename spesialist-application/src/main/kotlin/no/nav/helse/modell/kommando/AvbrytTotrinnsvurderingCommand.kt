package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt

internal class AvbrytTotrinnsvurderingCommand(
    private val identitetsnummer: Identitetsnummer,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val totrinnsvurdering = sessionContext.totrinnsvurderingRepository.finnAktivForPersonOrNull(identitetsnummer.value) ?: return true

        val aktiveVedtaksperioder =
            sessionContext.vedtaksperiodeRepository
                .finnAlleForPerson(identitetsnummer)
                .filterNot { it.forkastet }
                .map { AktivVedtaksperiode(it.id, sessionContext.behandlingRepository.finnNyesteForVedtaksperiode(it.id)) }

        val aktiveVedtaksperioderForOverstyringer = aktiveVedtaksperioderForOverstyringer(totrinnsvurdering, aktiveVedtaksperioder)
        val aktiveVedtaksperioderForSaksbehandlerhandlinger =
            sessionContext.saksbehandlerhandlingRepository
                .finnAlle(identitetsnummer)
                .filter { it.totrinnsvurderingId != null && it.totrinnsvurderingId == totrinnsvurdering.id() }
                .flatMap { it.aktiveVedtaksperioder(aktiveVedtaksperioder, sessionContext) }
        val aktiveVedtaksperioderForTilkomneInntekter =
            sessionContext.tilkommenInntektRepository
                .finnAlleForIdentitetsnummer(identitetsnummer)
                .filter { !it.fjernet && it.totrinnsvurderingId == totrinnsvurdering.id() }
                .flatMap { it.aktiveVedtaksperioder(aktiveVedtaksperioder) }

        val vedtaksperioderSomHolderTotrinnsvurderingenILive =
            (aktiveVedtaksperioderForOverstyringer + aktiveVedtaksperioderForSaksbehandlerhandlinger + aktiveVedtaksperioderForTilkomneInntekter).distinct()

        if (vedtaksperioderSomHolderTotrinnsvurderingenILive.isEmpty()) {
            loggInfo(
                "forkaster totrinnsvurderingen fordi alle relaterte vedtaksperioder er forkastet",
                "fødselsnummer" to identitetsnummer.value,
            )
            totrinnsvurdering.forkast()
        } else {
            val melding =
                if (totrinnsvurdering.beslutter == null) {
                    "Totrinnsvurderingen har ikke beslutter"
                } else {
                    "Beholder beslutter"
                }

            loggInfo(
                "Forkaster ikke totrinnsvurderingen, fordi den henger sammen med vedtaksperioder som ikke er forkastet: $vedtaksperioderSomHolderTotrinnsvurderingenILive. $melding",
                "fødselsnummer" to identitetsnummer.value,
            )
        }
        sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        return true
    }
}

private class AktivVedtaksperiode(
    val id: VedtaksperiodeId,
    val nyesteBehandling: Behandling?,
)

private fun aktiveVedtaksperioderForOverstyringer(
    totrinnsvurdering: Totrinnsvurdering,
    aktiveVedtaksperioder: List<AktivVedtaksperiode>,
): List<VedtaksperiodeId> {
    val vedtaksperiodeIder = totrinnsvurdering.overstyringer.map { VedtaksperiodeId(it.vedtaksperiodeId) }.toSet()
    return aktiveVedtaksperioder.map { it.id }.filter { it in vedtaksperiodeIder }
}

// behandlingId går foran skjæringstidspunkt. Uten noen av delene gjelder handlingen hele personen.
private fun Saksbehandlerhandling.aktiveVedtaksperioder(
    aktiveVedtaksperioder: List<AktivVedtaksperiode>,
    sessionContext: SessionContext,
): List<VedtaksperiodeId> {
    val aktiveIder = aktiveVedtaksperioder.map { it.id }
    val behandlingId = behandlingId
    val skjæringstidspunkt = skjæringstidspunkt
    return when {
        behandlingId != null -> {
            val behandling = sessionContext.behandlingRepository.finnOrNull(behandlingId)
            if (behandling == null) {
                loggInfo(
                    "Fant ikke behandling for saksbehandlerhandling, lar den gjelde hele personen",
                    "saksbehandlerhandlingId" to id.value,
                )
                aktiveIder
            } else {
                aktiveIder.filter { it == behandling.vedtaksperiodeId }
            }
        }

        skjæringstidspunkt != null ->
            aktiveVedtaksperioder
                .filter { it.nyesteBehandling?.skjæringstidspunkt == skjæringstidspunkt }
                .map { it.id }

        else -> aktiveIder
    }
}

private fun TilkommenInntekt.aktiveVedtaksperioder(aktiveVedtaksperioder: List<AktivVedtaksperiode>): List<VedtaksperiodeId> =
    aktiveVedtaksperioder
        .filter { aktiv ->
            val behandling = aktiv.nyesteBehandling ?: return@filter false
            (behandling.fom tilOgMed behandling.tom) overlapper periode
        }.map { it.id }
