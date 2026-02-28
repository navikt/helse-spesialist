package no.nav.helse.spesialist.application.snapshot

import java.math.BigDecimal

data class SnapshotPensjonsgivendeInntekt(
    val arligBelop: BigDecimal,
    val inntektsar: Int,
)
