package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.deaktiver
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.erTilbakedatert
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.finnBehandlingForVedtaksperiode
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.forhindrerAutomatisering
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.harKunGosysvarsel
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.harMedlemskapsvarsel
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.harVarselOmManglendeInntektsmelding
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.harÅpenGosysOppgave
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.håndterNyttVarsel
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.kreverSkjønnsfastsettelse
import java.time.LocalDate
import java.util.UUID

class Sykefraværstilfelle(
    private val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    private val gjeldendeBehandlinger: List<LegacyBehandling>,
) {
    init {
        check(gjeldendeBehandlinger.isNotEmpty()) { "Kan ikke opprette et sykefraværstilfelle uten behandlinger" }
    }

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

    fun håndter(varsel: LegacyVarsel) {
        gjeldendeBehandlinger.håndterNyttVarsel(listOf(varsel))
    }

    fun deaktiver(varsel: LegacyVarsel) {
        gjeldendeBehandlinger.deaktiver(varsel)
    }

    fun harMedlemskapsvarsel(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.harMedlemskapsvarsel(vedtaksperiodeId)

    fun manglerInntektsmelding(vedtaksperiodeId: UUID): Boolean =
        gjeldendeBehandlinger.harVarselOmManglendeInntektsmelding(
            vedtaksperiodeId,
        )

    fun kreverSkjønnsfastsettelse(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.kreverSkjønnsfastsettelse(vedtaksperiodeId)

    fun erTilbakedatert(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.erTilbakedatert(vedtaksperiodeId)

    fun harKunÅpenGosysOppgave(vedtaksperiodeId: UUID): Boolean = gjeldendeBehandlinger.harÅpenGosysOppgave(vedtaksperiodeId)
}
