package no.nav.helse.modell.risiko

import no.nav.helse.modell.vedtaksperiode.RisikovurderingForSpeilDto
import no.nav.helse.modell.automatisering.AutomatiseringValidering
import java.time.LocalDateTime
import java.util.*

class Risikovurdering private constructor(
    vedtaksperiodeId: UUID,
    opprettet: LocalDateTime,
    samletScore: Double,
    faresignaler: List<String>,
    private val arbeidsuførhetvurdering: List<String>,
    private val ufullstendig: Boolean
) : AutomatiseringValidering {
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

    fun speilDto() = RisikovurderingForSpeilDto(arbeidsuførhetvurdering, ufullstendig)

    override fun valider() = arbeidsuførhetvurdering.isEmpty() && !ufullstendig
    override fun error() = "Vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning er ikke oppfylt"
}

class RisikovurderingDto(
    val vedtaksperiodeId: UUID,
    val opprettet: LocalDateTime,
    val samletScore: Double,
    val faresignaler: List<String>,
    val arbeidsuførhetvurdering: List<String>,
    val ufullstendig: Boolean
)
