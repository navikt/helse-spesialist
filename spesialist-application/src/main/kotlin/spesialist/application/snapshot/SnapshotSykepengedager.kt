package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotSykepengedager(
    val forbrukteSykedager: Int?,
    val gjenstaendeSykedager: Int?,
    val maksdato: LocalDate,
    val oppfylt: Boolean,
    val skjaeringstidspunkt: LocalDate,
)
