package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate
import java.util.UUID

data class SnapshotAnnulleringskandidat(
    val fom: LocalDate,
    val organisasjonsnummer: String,
    val tom: LocalDate,
    val vedtaksperiodeId: UUID,
)
