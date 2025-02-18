package no.nav.helse.spesialist.db.dao

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.DokumentDao
import no.nav.helse.db.HelseDao
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import java.util.UUID
import javax.sql.DataSource

class PgDokumentDao private constructor(queryRunner: QueryRunner) : DokumentDao, QueryRunner by queryRunner {
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagre(
        fødselsnummer: String,
        dokumentId: UUID,
        dokument: JsonNode,
    ) {
        HelseDao.asSQL(
            """
            SELECT id FROM person WHERE fødselsnummer=:fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.int("id") }?.let { personId ->
            HelseDao.asSQL(
                """
                INSERT INTO dokumenter (dokument_id, person_ref, dokument)
                VALUES (
                    :dokumentId,
                    :personRef,
                    :dokument::json
                )
                ON CONFLICT (dokument_id) DO UPDATE SET dokument = excluded.dokument
                """.trimIndent(),
                "dokumentId" to dokumentId,
                "personRef" to personId,
                "dokument" to objectMapper.writeValueAsString(dokument),
            ).update()
        }
    }

    override fun hent(
        fødselsnummer: String,
        dokumentId: UUID,
    ): JsonNode? =
        HelseDao.asSQL(
            """
            SELECT dokument FROM dokumenter WHERE person_ref = (SELECT id FROM person WHERE fødselsnummer=:fodselsnummer) AND dokument_id =:dokumentId
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
            "dokumentId" to dokumentId,
        ).singleOrNull { row ->
            row.stringOrNull("dokument")?.let { dokument -> objectMapper.readTree(dokument) }
        }

    override fun slettGamleDokumenter() =
        HelseDao.asSQL(
            """
            delete from dokumenter where opprettet < current_date - interval '3 months';
            """.trimIndent(),
        ).update()
}
