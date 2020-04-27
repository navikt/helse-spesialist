package no.nav.helse.modell.dto

import java.util.UUID

data class SpleisbehovDBDto(
    val id: UUID,
    val spleisReferanse: UUID,
    val data: String
)
