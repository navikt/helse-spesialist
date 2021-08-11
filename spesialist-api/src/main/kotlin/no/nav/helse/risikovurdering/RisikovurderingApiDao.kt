package no.nav.helse.risikovurdering

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.HelseDao
import no.nav.helse.objectMapper
import java.util.*
import javax.sql.DataSource

class RisikovurderingApiDao(dataSource: DataSource): HelseDao(dataSource) {
    fun finnRisikovurdering(vedtaksperiodeId: UUID) =
        """SELECT data FROM risikovurdering_2021 WHERE vedtaksperiode_id = :vedtaksperiodeId ORDER BY id DESC LIMIT 1"""
            .single(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { objectMapper.readTree(it.string("data")) }
            ?.let { data -> RisikovurderingApiDto(funn = data["funn"].toList(), kontrollertOk = data["kontrollertOk"].toList()) }

    private fun JsonNode.toList() = objectMapper.readValue(
        traverse(),
        object : TypeReference<List<JsonNode>>() {}
    )
}
