package no.nav.helse.modell.risiko

import no.nav.helse.vedtaksperiode.RisikovurderingForSpeilDto
import java.time.LocalDateTime
import java.util.*

class Risikovurdering private constructor(
    vedtaksperiodeId: UUID,
    opprettet: LocalDateTime,
    samletScore: Double,
    faresignaler: List<String>,
    private val arbeidsuførhetvurdering: List<String>,
    private val ufullstendig: Boolean
) {
    companion object {
        fun restore(
            risikovurderingDto: RisikovurderingDto
        ) = Risikovurdering(
            vedtaksperiodeId = risikovurderingDto.vedtaksperiodeId,
            opprettet = risikovurderingDto.opprettet,
            samletScore = risikovurderingDto.samletScore,
            faresignaler = risikovurderingDto.faresignaler,
            arbeidsuførhetvurdering = risikovurderingDto.arbeidsuførhetvurdering,
            ufullstendig = risikovurderingDto.ufullstendig
        )
    }

    fun kanBehandlesAutomatisk() = arbeidsuførhetvurdering.isEmpty() && !ufullstendig
    fun speilVariant() = RisikovurderingForSpeilDto(arbeidsuførhetvurdering, ufullstendig)
}

class RisikovurderingDto(
    val vedtaksperiodeId: UUID,
    val opprettet: LocalDateTime,
    val samletScore: Double,
    val faresignaler: List<String>,
    val arbeidsuførhetvurdering: List<String>,
    val ufullstendig: Boolean
)
