package no.nav.helse.modell.dokument

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao
import no.nav.helse.objectMapper

class DokumentDao(private val dataSource: DataSource) : HelseDao(dataSource) {
    internal fun lagre(fødselsnummer: String, dokumentId: UUID, dokument: JsonNode) {
        asSQL(
            """
            INSERT INTO dokumenter (dokument_id, person_ref, dokument)
            VALUES (
                :dokumentId,
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                :dokument::json
            )
            ON CONFLICT DO NOTHING
        """.trimIndent(), mapOf(
                "fodselsnummer" to fødselsnummer.toLong(),
                "dokumentId" to dokumentId,
                "dokument" to objectMapper.writeValueAsString(dokument)
            )
        ).update()
    }

    internal fun hent(fødselsnummer: String, dokumentId: UUID): JsonNode? = asSQL(
        """
            SELECT dokument FROM dokumenter WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer=:fodselsnummer) AND dokument_id =:dokumentId
        """.trimIndent(), mapOf(
            "fodselsnummer" to fødselsnummer.toLong(),
            "dokumentId" to dokumentId,
        )
    ).single { row ->
        row.stringOrNull("dokument")?.let { dokument -> objectMapper.readTree(dokument) }
    }
}