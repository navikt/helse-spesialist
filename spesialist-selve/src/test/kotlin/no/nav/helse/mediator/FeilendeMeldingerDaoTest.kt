package no.nav.helse.mediator

import DatabaseIntegrationTest
import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class FeilendeMeldingerDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `lagrer feilet melding`() {
        val hendelse = "EN_HENDELSE"
        val id = UUID.randomUUID()
        @Language("JSON")
        val json = """
            {
              "@id": "$id",
              "@event_name": "$hendelse"
            }
        """
        feilendeMeldingerDao.lagre(id, hendelse, json)
        feilendeMeldinger(id).first().also {
            assertEquals(id, it.id)
            assertEquals(hendelse, it.eventName)
            assertNotNull(it.opprettet)
            assertEquals(id, UUID.fromString(it.json["@id"].asText()))
        }
    }

    @Test
    fun `feiler ikke om vi forsøker å lagre samme melding flere ganger`() {
        val hendelse = "duplikat_hendelse"
        val id = UUID.randomUUID()
        @Language("JSON")
        val json = """
            {
              "@id": "${UUID.randomUUID()}",
              "@event_name": "duplikat_hendelse"
            }
        """
        feilendeMeldingerDao.lagre(id, hendelse, json)
        feilendeMeldingerDao.lagre(id, hendelse, json)
    }

    private fun feilendeMeldinger(id: UUID): List<FeiletMelding> {
        @Language("PostgreSQL")
        val statement = "SELECT * FROM feilende_meldinger WHERE id = :id"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf("id" to id)).map {
                FeiletMelding(
                    id = UUID.fromString(it.string("id")),
                    eventName = it.string("event_name"),
                    json = objectMapper.readTree(it.string("blob")),
                    opprettet = it.localDateTime("opprettet")
                )
            }.asList)
        }
    }

    private data class FeiletMelding(
        val id: UUID,
        val eventName: String,
        val json: JsonNode,
        val opprettet: LocalDateTime
    )
}
