package no.nav.helse.modell.risiko

import java.time.LocalDateTime
import java.util.*

class Risikovurdering private constructor(
    private val vedtaksperiodeId: UUID,
    private val opprettet: LocalDateTime,
    private val samletScore: Int,
    private val faresignaler: List<String>,
    private val arbeidsuførhetvurdering: List<String>,
    private val ufullstendig: Boolean
) {
    companion object {
        fun restore(
            vedtaksperiodeId: UUID,
            opprettet: LocalDateTime,
            samletScore: Int,
            faresignaler: List<String>,
            arbeidsuførhetvurdering: List<String>,
            ufullstendig: Boolean
        ) = Risikovurdering(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = opprettet,
            samletScore = samletScore,
            faresignaler = faresignaler,
            arbeidsuførhetvurdering = arbeidsuførhetvurdering,
            ufullstendig = ufullstendig
        )
    }

    fun kanBehandlesAutomatisk(): Boolean {
        return TODO()
    }
}

class RisikovurderingDto(
    val vedtaksperiodeId: UUID,
    val opprettet: LocalDateTime,
    val samletScore: Int,
    val faresignaler: List<String>,
    val arbeidsuførhetvurdering: List<String>,
    val ufullstendig: Boolean
)
