package no.nav.helse.modell.dokument

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Session
import kotliquery.sessionOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.single
import no.nav.helse.HelseDao.Companion.update
import no.nav.helse.objectMapper
import java.util.UUID
import javax.sql.DataSource

internal interface DokumentDaoInterface {
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

internal class DokumentDao(private val session: Session) : DokumentDaoInterface {
    internal object NonTransactional {
        operator fun invoke(dataSource: DataSource): DokumentDaoInterface {
            fun <T> inSession(block: (Session) -> T) = sessionOf(dataSource).use { block(it) }

            return object : DokumentDaoInterface {
                override fun lagre(
                    fødselsnummer: String,
                    dokumentId: UUID,
                    dokument: JsonNode,
                ) {
                    inSession { DokumentDao(it).lagre(fødselsnummer, dokumentId, dokument) }
                }

                override fun hent(
                    fødselsnummer: String,
                    dokumentId: UUID,
                ) = inSession { DokumentDao(it).hent(fødselsnummer, dokumentId) }

                override fun slettGamleDokumenter(): Int = inSession { DokumentDao(it).slettGamleDokumenter() }
            }
        }
    }

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
        ).single(session) { it.int("id") }?.let { personId ->
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
            ).update(session)
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
        ).single(session) { row ->
            row.stringOrNull("dokument")?.let { dokument -> objectMapper.readTree(dokument) }
        }

    override fun slettGamleDokumenter() =
        asSQL(
            """
            delete from dokumenter where opprettet < current_date - interval '3 months';
            """.trimIndent(),
        ).update(session)
}
