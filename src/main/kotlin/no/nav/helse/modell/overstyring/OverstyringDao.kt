package no.nav.helse.modell.overstyring

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.arbeidsgiver.findArbeidsgiverByOrgnummer
import no.nav.helse.modell.person.toFødselsnummer
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime
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

    fun finnOverstyring(
        fødselsnummer: String,
        organisasjonsnummer: String
    ) = sessionOf(dataSource).use { it.finnOverstyring(fødselsnummer, organisasjonsnummer) }

    private fun Session.persisterOverstyring(
        hendelseId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        saksbehandlerRef: UUID
    ): Long? {
        @Language("PostgreSQL")
        val opprettOverstyringQuery = """
        INSERT INTO overstyring(hendelse_id, person_ref, arbeidsgiver_ref, begrunnelse, saksbehandler_ref)
        VALUES (:hendelse_id,
                :person_ref,
                :arbeidsgiver_ref,
                :begrunnelse,
                :saksbehandler_ref)
    """

        @Language("PostgreSQL")
        val opprettOverstyringDagQuery = """
        INSERT INTO overstyrtdag(overstyring_ref, dato, dagtype, grad)
        VALUES (:overstyring_ref,
                :dato,
                :dagtype,
                :grad)
    """

        val person_ref = findPersonByFødselsnummer(fødselsnummer)
        val arbeidsgiver_ref = findArbeidsgiverByOrgnummer(organisasjonsnummer)

        val overstyringRef = this.run(
            queryOf(
                opprettOverstyringQuery,
                mapOf(
                    "hendelse_id" to hendelseId,
                    "person_ref" to person_ref,
                    "arbeidsgiver_ref" to arbeidsgiver_ref,
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
        return overstyringRef
    }

    private fun Session.findPersonByFødselsnummer(fødselsnummer: String): Int? = this.run(
        queryOf("SELECT id FROM person WHERE fodselsnummer=?;", fødselsnummer.toLong())
            .map { it.int("id") }
            .asSingle
    )

    private fun Session.finnOverstyring(fødselsnummer: String, organisasjonsnummer: String): List<OverstyringDto> {
        @Language("PostgreSQL")
        val finnOverstyringQuery = """
SELECT o.*, p.fodselsnummer, a.orgnummer, s.navn
FROM overstyring o
         INNER JOIN person p ON p.id = o.person_ref
         INNER JOIN arbeidsgiver a on a.id = o.arbeidsgiver_ref
         INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
WHERE p.fodselsnummer = ?
  AND a.orgnummer = ?
    """
        return this.run(
            queryOf(
                finnOverstyringQuery,
                fødselsnummer.toLong(),
                organisasjonsnummer.toLong()
            ).map { overstyringRow ->
                val id = overstyringRow.long("id")

                OverstyringDto(
                    hendelseId = UUID.fromString(overstyringRow.string("hendelse_id")),
                    fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
                    organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
                    begrunnelse = overstyringRow.string("begrunnelse"),
                    timestamp = overstyringRow.localDateTime("tidspunkt"),
                    saksbehandlerNavn = overstyringRow.string("navn"),
                    overstyrteDager = this.run(
                        queryOf(
                            "SELECT * FROM overstyrtdag WHERE overstyring_ref = ?", id
                        ).map { overstyringDagRow ->
                            OverstyringDagDto(
                                dato = overstyringDagRow.localDate("dato"),
                                type = enumValueOf(overstyringDagRow.string("dagtype")),
                                grad = overstyringDagRow.intOrNull("grad")
                            )
                        }.asList
                    )
                )
            }.asList
        )
    }
}

data class OverstyringDto(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val begrunnelse: String,
    val timestamp: LocalDateTime,
    val saksbehandlerNavn: String,
    val overstyrteDager: List<OverstyringDagDto>
)

data class OverstyringDagDto(
    val dato: LocalDate,
    val type: Dagtype,
    val grad: Int?
)

enum class Dagtype { Sykedag, Feriedag, Egenmeldingsdag, Permisjonsdag }
