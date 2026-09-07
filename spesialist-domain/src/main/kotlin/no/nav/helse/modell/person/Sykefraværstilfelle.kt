package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling.Companion.håndterNyttVarsel
import java.time.LocalDate

class Sykefraværstilfelle(
    private val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    private val gjeldendeBehandlinger: List<LegacyBehandling>,
) {
    init {
        check(gjeldendeBehandlinger.isNotEmpty()) { "Kan ikke opprette et sykefraværstilfelle uten behandlinger" }
    }

    fun håndter(varsel: LegacyVarsel) {
        gjeldendeBehandlinger.håndterNyttVarsel(listOf(varsel))
    }
}
