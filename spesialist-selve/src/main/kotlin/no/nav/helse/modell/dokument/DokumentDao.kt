package no.nav.helse.modell.dokument

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

class DokumentDao(private val dataSource: DataSource) {
    internal fun lagre(fødselsnummer: String, dokumentId: UUID, dokument: JsonNode) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO dokumenter (dokument_id, person_ref, dokument)
            VALUES (
                :dokumentId,
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                :dokument::json
            )
            ON CONFLICT DO NOTHING
        """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "dokumentId" to dokumentId,
                        "dokument" to objectMapper.writeValueAsString(dokument)
                    )
                ).asExecute
            )
        }
    }

    internal fun hent(fødselsnummer: String, dokumentId: UUID): JsonNode? {
        @Language("PostgreSQL")
        val query = """
            SELECT dokument FROM dokumenter WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer=:fodselsnummer) AND dokument_id =:dokumentId
        """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "dokumentId" to dokumentId,
                    )
                )
                    .map {
                        it.stringOrNull("dokument")?.let { dokument -> objectMapper.readTree(dokument) }
                    }
                    .asSingle
            )
        }
    }
}