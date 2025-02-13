package no.nav.helse.spesialist.application.snapshot

import java.util.UUID

data class SnapshotGenerasjon(
    val id: UUID,
    val perioder: List<SnapshotTidslinjeperiode>,
)
