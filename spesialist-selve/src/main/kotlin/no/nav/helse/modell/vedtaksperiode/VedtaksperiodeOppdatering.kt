package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeOppdatering(
    val fom: LocalDate,
    val tom: LocalDate,
    val skjæringstidspunkt: LocalDate,
    val vedtaksperiodeId: UUID
)