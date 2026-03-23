package no.nav.helse.spesialist.application.snapshot

import java.time.YearMonth

data class SnapshotInntekterFraAOrdningen(
    val maned: YearMonth,
    val sum: Double,
)
