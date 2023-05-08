package no.nav.helse.spesialist.api.vedtak

import java.time.LocalDate
import java.util.UUID

data class ApiVedtak(
    private val vedtaksperiodeId: UUID,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val skjæringstidspunkt: LocalDate
) {
    internal fun vedtaksperiodeId() = this.vedtaksperiodeId

    internal fun tidligereEnnOgSammenhengende(other: ApiVedtak): Boolean = this.fom <= other.tom && this.skjæringstidspunkt == other.skjæringstidspunkt
}