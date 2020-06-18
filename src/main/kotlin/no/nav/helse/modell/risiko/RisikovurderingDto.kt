package no.nav.helse.modell.risiko

import java.time.LocalDateTime
import java.util.*

data class RisikovurderingDto(
    val vedtaksperiodeId: UUID,
    val opprettet: LocalDateTime,
    val samletScore: Int,
    val faresignaler: List<String>,
    val arbeidsuførhetvurdering: List<String>,
    val ufullstendig: Boolean
)
