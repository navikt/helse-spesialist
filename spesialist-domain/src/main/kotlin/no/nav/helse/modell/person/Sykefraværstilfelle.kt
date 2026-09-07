package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.finnBehandlingForVedtaksperiode
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.forhindrerAutomatisering
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.håndterNyttVarsel
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

    fun forhindrerAutomatisering(vedtaksperiodeId: UUID): Boolean {
        val behandlingForPeriode =
            gjeldendeBehandlinger.finnBehandlingForVedtaksperiode(vedtaksperiodeId)
                ?: throw IllegalStateException("Sykefraværstilfellet må inneholde behandling for vedtaksperiodeId=$vedtaksperiodeId")
        return gjeldendeBehandlinger.forhindrerAutomatisering(behandlingForPeriode)
    }

    fun håndter(varsel: LegacyVarsel) {
        gjeldendeBehandlinger.håndterNyttVarsel(listOf(varsel))
    }
}
