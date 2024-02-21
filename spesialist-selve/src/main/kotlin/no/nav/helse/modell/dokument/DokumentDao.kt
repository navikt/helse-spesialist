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
                SELECT id FROM person WHERE fodselsnummer=:fodselsnummer
            """.trimIndent(), mapOf(
                "fodselsnummer" to fødselsnummer.toLong(),
            )
        ).single { it.int("id") }?.let { personId ->
            asSQL(
                """
            INSERT INTO dokumenter (dokument_id, person_ref, dokument)
            VALUES (
                :dokumentId,
                :personRef,
                :dokument::json
            )
            ON CONFLICT (dokument_id) DO UPDATE SET dokument = excluded.dokument
        """.trimIndent(), mapOf(
                    "dokumentId" to dokumentId,
                    "personRef" to personId,
                    "dokument" to objectMapper.writeValueAsString(dokument)
                )
            ).update()
        }
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

    internal fun slettGamleDokumenter() = asSQL(
        """
            DELETE FROM dokumenter where opprettet < CURRENT_DATE - interval '3 months';
        """.trimIndent()
    ).update()
}