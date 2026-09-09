package no.nav.helse.db.api

import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeDbDto(
    val vedtaksperiodeId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val tags: Set<String>,
    val varsler: Set<VarselDbDto>,
) {
    fun tidligereEnnOgSammenhengende(other: VedtaksperiodeDbDto): Boolean {
        val overlapperEllerKantIKant = this.fom <= other.tom
        val sammeSkjæringstidspunkt = this.skjæringstidspunkt == other.skjæringstidspunkt
        val oppholdInntil18Dager = this.tom in other.fom.minusDays(18)..other.fom
        return (overlapperEllerKantIKant && sammeSkjæringstidspunkt) || oppholdInntil18Dager
    }
}
