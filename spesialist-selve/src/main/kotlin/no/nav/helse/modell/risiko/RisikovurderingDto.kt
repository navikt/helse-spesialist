package no.nav.helse.modell.risiko

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.automatisering.AutomatiseringValidering
import no.nav.helse.modell.vedtaksperiode.RisikovurderingForSpeilDto
import no.nav.helse.objectMapper
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

    fun speilDto() = RisikovurderingForSpeilDto(toList(data["funn"]), toList(data["kontrollertOk"]))

    private fun toList(node: JsonNode) = objectMapper.readValue(
        node.traverse(),
        object : TypeReference<List<JsonNode>>() {}
    )

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
