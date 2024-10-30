package no.nav.helse.spesialist.api.risikovurdering

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.api.objectMapper
import java.util.UUID
import javax.sql.DataSource

class RisikovurderingApiDao(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    fun finnRisikovurdering(vedtaksperiodeId: UUID): RisikovurderingApiDto? =
        asSQL(
            " SELECT data FROM risikovurdering_2021 WHERE vedtaksperiode_id = :vedtaksperiodeId ORDER BY id DESC LIMIT 1 ",
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { objectMapper.readTree(it.string("data")) }?.let { data ->
            RisikovurderingApiDto(funn = data["funn"].toList(), kontrollertOk = data["kontrollertOk"].toList())
        }

    private fun JsonNode.toList(): List<JsonNode> =
        objectMapper.readValue(
            traverse(),
            object : TypeReference<List<JsonNode>>() {},
        )
}
