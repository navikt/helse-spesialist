package no.nav.helse.risikovurdering

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class RisikovurderingApiDao(private val dataSource: DataSource) {
    fun finnRisikovurdering(vedtaksperiodeId: UUID) = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = "SELECT data FROM risikovurdering_2021 WHERE vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1"

            session.run(
                queryOf(statement, vedtaksperiodeId).map {
                    objectMapper.readTree(it.string("data"))
                }.asSingle
            )?.let { data ->
                RisikovurderingApiDto(
                    funn = data["funn"].toList(),
                    kontrollertOk = data["kontrollertOk"].toList()
                )
            }
        }

    private fun JsonNode.toList() = objectMapper.readValue(
        traverse(),
        object : TypeReference<List<JsonNode>>() {}
    )
}
