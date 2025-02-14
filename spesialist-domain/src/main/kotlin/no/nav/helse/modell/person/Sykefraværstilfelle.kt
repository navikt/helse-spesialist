package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.Behandling
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.deaktiver
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.erTilbakedatert
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.finnBehandlingForVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.forhindrerAutomatisering
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.harKunGosysvarsel
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.harMedlemskapsvarsel
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.harVarselOmManglendeInntektsmelding
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.harÅpenGosysOppgave
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.håndterGodkjent
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.håndterNyttVarsel
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.kreverSkjønnsfastsettelse
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import java.time.LocalDate
import java.util.UUID

class Sykefraværstilfelle(
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val gjeldendeBehandlinger: List<Behandling>,
) {
    init {
        check(gjeldendeBehandlinger.isNotEmpty()) { "Kan ikke opprette et sykefraværstilfelle uten behandlinger" }
    }

    fun skjæringstidspunkt() = skjæringstidspunkt

    fun haster(vedtaksperiodeId: UUID): Boolean {
        val behandling =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalArgumentException(
                    "Finner ikke behandling med vedtaksperiodeId=$vedtaksperiodeId i sykefraværstilfelle med skjæringstidspunkt=$skjæringstidspunkt",
                )
        return behandling.hasterÅBehandle()
    }

    fun forhindrerAutomatisering(vedtaksperiodeId: UUID): Boolean {
        val behandlingForPeriode =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde behandling for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeBehandlinger.forhindrerAutomatisering(behandlingForPeriode)
    }

    fun harKunGosysvarsel(vedtaksperiodeId: UUID): Boolean {
        val behandlingForPeriode =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde behandling for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeBehandlinger.harKunGosysvarsel(behandlingForPeriode)
    }

    fun harVarselOmManglendeInntektsmelding(vedtaksperiodeId: UUID): Boolean {
        val behandlingForPeriode =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde behandling for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeBehandlinger.harVarselOmManglendeInntektsmelding(behandlingForPeriode)
    }

    fun håndter(varsel: Varsel) {
        gjeldendeBehandlinger.håndterNyttVarsel(listOf(varsel))
    }

    fun deaktiver(varsel: Varsel) {
        gjeldendeBehandlinger.deaktiver(varsel)
    }

    fun håndterGodkjent(vedtaksperiodeId: UUID) {
        gjeldendeBehandlinger.håndterGodkjent(vedtaksperiodeId)
    }

    fun harMedlemskapsvarsel(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.harMedlemskapsvarsel(vedtaksperiodeId)

    fun kreverSkjønnsfastsettelse(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.kreverSkjønnsfastsettelse(vedtaksperiodeId)

    fun erTilbakedatert(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.erTilbakedatert(vedtaksperiodeId)

    fun harKunÅpenGosysOppgave(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.harÅpenGosysOppgave(vedtaksperiodeId)
}
