package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDateTime

data class SnapshotVurdering(
    val automatisk: Boolean,
    val godkjent: Boolean,
    val ident: String,
    val tidsstempel: LocalDateTime,
)
