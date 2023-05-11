package no.nav.helse.spesialist.api.vedtak

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.varsel.Varsel

data class ApiGenerasjon(
    private val vedtaksperiodeId: UUID,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val skjæringstidspunkt: LocalDate,
    private val varsler: Set<Varsel>
) {
    internal fun vedtaksperiodeId() = this.vedtaksperiodeId

    internal fun tidligereEnnOgSammenhengende(other: ApiGenerasjon): Boolean = this.fom <= other.tom && this.skjæringstidspunkt == other.skjæringstidspunkt

    private fun harAktiveVarsler(): Boolean {
        return varsler.any { it.erAktiv() }
    }

    companion object {
        fun Set<ApiGenerasjon>.harAktiveVarsler(): Boolean {
            return any { it.harAktiveVarsler() }
        }
    }
}