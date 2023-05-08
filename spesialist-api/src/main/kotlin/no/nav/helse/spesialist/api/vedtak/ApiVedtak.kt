package no.nav.helse.spesialist.api.vedtak

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

data class ApiVedtak(
    private val vedtaksperiodeId: UUID,
    private val fom: LocalDate,
    private val tom: LocalDate,
) {
    internal fun vedtaksperiodeId() = this.vedtaksperiodeId

    internal fun erPeriodeRettFør(periode: ApiVedtak): Boolean = this.tom.erRettFør(periode.fom)

    private fun LocalDate.erRettFør(other: LocalDate): Boolean {
        if (this >= other) return false
        if (this.nesteDag == other) return true
        return when (this.dayOfWeek) {
            DayOfWeek.FRIDAY -> other in this.plusDays(2)..this.plusDays(3)
            DayOfWeek.SATURDAY -> other == this.plusDays(2)
            else -> false
        }
    }

    private val LocalDate.nesteDag get() = this.plusDays(1)
}