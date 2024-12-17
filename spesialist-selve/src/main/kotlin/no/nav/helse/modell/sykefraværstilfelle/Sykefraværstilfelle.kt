package no.nav.helse.modell.sykefraværstilfelle

import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.vedtaksperiode.Behandling
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.deaktiver
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.erTilbakedatert
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.finnBehandlingForVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.forhindrerAutomatisering
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.harKunGosysvarsel
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.harMedlemskapsvarsel
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.harVarselOmManglendeInntektsmelding
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.harÅpenGosysOppgave
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.håndterGodkjent
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.håndterNyttVarsel
import no.nav.helse.modell.vedtaksperiode.Behandling.Companion.kreverSkjønnsfastsettelse
import java.time.LocalDate
import java.util.UUID

internal class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val gjeldendeBehandlinger: List<Behandling>,
) {
    init {
        check(gjeldendeBehandlinger.isNotEmpty()) { "Kan ikke opprette et sykefraværstilfelle uten behandlinger" }
    }

    internal fun skjæringstidspunkt() = skjæringstidspunkt

    internal fun haster(vedtaksperiodeId: UUID): Boolean {
        val behandling =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalArgumentException(
                    "Finner ikke behandling med vedtaksperiodeId=$vedtaksperiodeId i sykefraværstilfelle med skjæringstidspunkt=$skjæringstidspunkt",
                )
        return behandling.hasterÅBehandle()
    }

    internal fun forhindrerAutomatisering(vedtaksperiodeId: UUID): Boolean {
        val behandlingForPeriode =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde behandling for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeBehandlinger.forhindrerAutomatisering(behandlingForPeriode)
    }

    internal fun harKunGosysvarsel(vedtaksperiodeId: UUID): Boolean {
        val behandlingForPeriode =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde behandling for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeBehandlinger.harKunGosysvarsel(behandlingForPeriode)
    }

    internal fun harVarselOmManglendeInntektsmelding(vedtaksperiodeId: UUID): Boolean {
        val behandlingForPeriode =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde behandling for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeBehandlinger.harVarselOmManglendeInntektsmelding(behandlingForPeriode)
    }

    internal fun håndter(varsel: Varsel) {
        gjeldendeBehandlinger.håndterNyttVarsel(listOf(varsel))
    }

    internal fun deaktiver(varsel: Varsel) {
        gjeldendeBehandlinger.deaktiver(varsel)
    }

    internal fun håndterGodkjent(vedtaksperiodeId: UUID) {
        gjeldendeBehandlinger.håndterGodkjent(vedtaksperiodeId)
    }

    internal fun harMedlemskapsvarsel(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.harMedlemskapsvarsel(vedtaksperiodeId)

    internal fun kreverSkjønnsfastsettelse(vedtaksperiodeId: UUID): Boolean =
        gjeldendeBehandlinger.kreverSkjønnsfastsettelse(vedtaksperiodeId)

    internal fun erTilbakedatert(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.erTilbakedatert(vedtaksperiodeId)

    internal fun harKunÅpenGosysOppgave(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.harÅpenGosysOppgave(vedtaksperiodeId)
}
