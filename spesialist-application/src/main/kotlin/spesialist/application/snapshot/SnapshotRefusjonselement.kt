package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate
import java.util.UUID

data class SnapshotRefusjonselement(
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: Double,
    val meldingsreferanseId: UUID,
)
