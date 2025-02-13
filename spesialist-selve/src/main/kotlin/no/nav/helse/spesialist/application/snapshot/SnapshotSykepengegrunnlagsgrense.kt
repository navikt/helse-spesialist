package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotSykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate,
)
