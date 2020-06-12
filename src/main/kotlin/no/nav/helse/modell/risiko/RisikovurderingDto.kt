package no.nav.helse.modell.risiko

import java.util.*

data class RisikovurderingDto(
    val vedtaksperiodeId: UUID,
    val samletScore: Int,
    val begrunnelser: List<String>,
    val ufullstendig: Boolean
)
