package no.nav.helse.spesialist.application.snapshot

import java.util.UUID

data class SnapshotSykdomsdagkilde(
    val id: UUID,
    val type: SnapshotSykdomsdagkildetype,
)
