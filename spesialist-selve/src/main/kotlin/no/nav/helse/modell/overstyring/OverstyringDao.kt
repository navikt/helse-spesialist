package no.nav.helse.modell.overstyring

import kotliquery.Session
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
            session.persisterOverstyring(
                hendelseId,
                fødselsnummer,
                organisasjonsnummer,
                begrunnelse,
                overstyrteDager,
                saksbehandlerRef
            )
        }
    }

    private fun Session.persisterOverstyring(
        hendelseId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        saksbehandlerRef: UUID
    ) {
        @Language("PostgreSQL")
        val opprettOverstyringQuery = """
        INSERT INTO overstyring(hendelse_id, person_ref, arbeidsgiver_ref, begrunnelse, saksbehandler_ref)
            SELECT :hendelse_id, p.id, ag.id, :begrunnelse, :saksbehandler_ref
            FROM arbeidsgiver ag, person p
            WHERE p.fodselsnummer=:fodselsnummer
            AND ag.orgnummer=:orgnr
    """

        @Language("PostgreSQL")
        val opprettOverstyringDagQuery = """
        INSERT INTO overstyrtdag(overstyring_ref, dato, dagtype, grad)
        VALUES (:overstyring_ref,
                :dato,
                :dagtype,
                :grad)
    """
        val overstyringRef = this.run(
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

        this.transaction { transactionalSession ->
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
