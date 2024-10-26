package no.nav.helse.modell.dokument

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.objectMapper
import java.util.UUID
import javax.sql.DataSource

internal interface DokumentDao {
    fun lagre(
        fødselsnummer: String,
        dokumentId: UUID,
        dokument: JsonNode,
    )

    fun hent(
        fødselsnummer: String,
        dokumentId: UUID,
    ): JsonNode?

    fun slettGamleDokumenter(): Int
}

internal class PgDokumentDao(queryRunner: QueryRunner) : DokumentDao, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagre(
        fødselsnummer: String,
        dokumentId: UUID,
        dokument: JsonNode,
    ) {
        asSQL(
            """
            SELECT id FROM person WHERE fodselsnummer=:fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
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
        asSQL(
            """
            SELECT dokument FROM dokumenter WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer=:fodselsnummer) AND dokument_id =:dokumentId
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
            "dokumentId" to dokumentId,
        ).single { row ->
            row.stringOrNull("dokument")?.let { dokument -> objectMapper.readTree(dokument) }
        }

    override fun slettGamleDokumenter() =
        asSQL(
            """
            delete from dokumenter where opprettet < current_date - interval '3 months';
            """.trimIndent(),
        ).update()
}
