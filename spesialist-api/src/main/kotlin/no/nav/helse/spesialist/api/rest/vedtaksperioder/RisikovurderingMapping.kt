package no.nav.helse.spesialist.api.rest.vedtaksperioder

import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.rest.saksbilde.ApiFaresignal
import no.nav.helse.spesialist.api.rest.saksbilde.ApiRisikovurdering
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import tools.jackson.databind.JsonNode

internal fun RisikovurderingApiDto.tilApiRisikovurdering(): ApiRisikovurdering =
    ApiRisikovurdering(
        funn = funn.tilFaresignaler(),
        kontrollertOk = kontrollertOk.tilFaresignaler(),
    )

private fun List<JsonNode>.tilFaresignaler(): List<ApiFaresignal> = map { objectMapper.treeToValue(it, ApiFaresignal::class.java) }
