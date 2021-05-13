package no.nav.helse.modell.risiko

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.automatisering.AutomatiseringValidering
import java.time.LocalDateTime
import java.util.*


class Risikovurdering private constructor(
    private val kanGodkjennesAutomatisk: Boolean,
    private val data: JsonNode,
) : AutomatiseringValidering {
    companion object {
        fun restore(risikovurderingDto: RisikovurderingDto) = Risikovurdering(
            risikovurderingDto.kanGodkjennesAutomatisk,
            risikovurderingDto.data
        )
    }

    override fun erAautomatiserbar() = kanGodkjennesAutomatisk
    override fun error() = "Vilkårsvurdering for arbeidsuførhet, aktivitetsplikt eller medvirkning er ikke oppfylt"
}

class RisikovurderingDto(
    val vedtaksperiodeId: UUID,
    val kanGodkjennesAutomatisk: Boolean,
    val kreverSupersaksbehandler: Boolean,
    val opprettet: LocalDateTime,
    val data: JsonNode,
)
