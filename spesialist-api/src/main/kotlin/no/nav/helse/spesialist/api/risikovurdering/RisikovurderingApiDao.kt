package no.nav.helse.spesialist.api.risikovurdering

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.objectMapper

class RisikovurderingApiDao(dataSource: DataSource): HelseDao(dataSource) {
    fun finnRisikovurdering(vedtaksperiodeId: UUID): RisikovurderingApiDto? = asSQL(
        " SELECT data FROM risikovurdering_2021 WHERE vedtaksperiode_id = :vedtaksperiodeId ORDER BY id DESC LIMIT 1; ",
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { objectMapper.readTree(it.string("data")) }?.let { data ->
        RisikovurderingApiDto(funn = data["funn"].toList(), kontrollertOk = data["kontrollertOk"].toList())
    }

    private fun JsonNode.toList(): List<JsonNode> = objectMapper.readValue(
        traverse(),
        object : TypeReference<List<JsonNode>>() {}
    )
}
