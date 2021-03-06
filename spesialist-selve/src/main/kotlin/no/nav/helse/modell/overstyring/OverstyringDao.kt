package no.nav.helse.modell.overstyring

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.overstyring.OverstyringDagDto
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class OverstyringDao(private val dataSource: DataSource) {
    fun persisterOverstyring(
        hendelseId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        saksbehandlerRef: UUID
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery = """
                INSERT INTO overstyring(hendelse_id, person_ref, arbeidsgiver_ref, begrunnelse, saksbehandler_ref)
                SELECT :hendelse_id, p.id, ag.id, :begrunnelse, :saksbehandler_ref
                FROM arbeidsgiver ag,
                     person p
                WHERE p.fodselsnummer = :fodselsnummer
                  AND ag.orgnummer = :orgnr
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringDagQuery = """
                INSERT INTO overstyrtdag(overstyring_ref, dato, dagtype, grad)
                VALUES (:overstyring_ref, :dato, :dagtype, :grad)
            """.trimIndent()
            val overstyringRef = session.run(
                queryOf(
                    opprettOverstyringQuery,
                    mapOf(
                        "hendelse_id" to hendelseId,
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "orgnr" to organisasjonsnummer.toLong(),
                        "begrunnelse" to begrunnelse,
                        "saksbehandler_ref" to saksbehandlerRef
                    )
                ).asUpdateAndReturnGeneratedKey
            )
            session.transaction { transactionalSession ->
                overstyrteDager.forEach { dag ->
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringDagQuery,
                            mapOf(
                                "overstyring_ref" to overstyringRef,
                                "dato" to dag.dato,
                                "dagtype" to dag.type.toString(),
                                "grad" to dag.grad
                            )
                        ).asUpdate
                    )
                }
            }
        }
    }

}
