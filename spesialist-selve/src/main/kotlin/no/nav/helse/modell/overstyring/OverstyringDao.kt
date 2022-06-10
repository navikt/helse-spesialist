package no.nav.helse.modell.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.overstyring.OverstyringDagDto
import org.intellij.lang.annotations.Language

class OverstyringDao(private val dataSource: DataSource) {
    fun persisterOverstyringTidslinje(
        hendelseId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        saksbehandlerRef: UUID,
        tidspunkt: LocalDateTime
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery = """
                INSERT INTO overstyring(hendelse_id, person_ref, arbeidsgiver_ref, begrunnelse, saksbehandler_ref, tidspunkt)
                SELECT :hendelse_id, p.id, ag.id, :begrunnelse, :saksbehandler_ref, :tidspunkt
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
                        "saksbehandler_ref" to saksbehandlerRef,
                        "tidspunkt" to tidspunkt
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

    fun persisterOverstyringInntekt(
        hendelseId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        begrunnelse: String,
        forklaring: String,
        saksbehandlerRef: UUID,
        månedligInntekt: Double,
        skjæringstidspunkt: LocalDate,
        tidspunkt: LocalDateTime
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery = """
                INSERT INTO overstyring_inntekt(hendelse_ref, person_ref, arbeidsgiver_ref, saksbehandler_ref, begrunnelse, forklaring, manedlig_inntekt, skjaeringstidspunkt, tidspunkt)
                SELECT :hendelse_id, p.id, ag.id, :saksbehandler_ref, :begrunnelse, :forklaring, :manedlig_inntekt, :skjaeringstidspunkt, :tidspunkt
                FROM arbeidsgiver ag,
                     person p
                WHERE p.fodselsnummer = :fodselsnummer
                  AND ag.orgnummer = :orgnr
            """.trimIndent()
            session.run(
                queryOf(
                    opprettOverstyringQuery,
                    mapOf(
                        "hendelse_id" to hendelseId,
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "orgnr" to organisasjonsnummer.toLong(),
                        "saksbehandler_ref" to saksbehandlerRef,
                        "begrunnelse" to begrunnelse,
                        "forklaring" to forklaring,
                        "manedlig_inntekt" to månedligInntekt,
                        "skjaeringstidspunkt" to skjæringstidspunkt,
                        "tidspunkt" to tidspunkt

                    )
                ).asUpdateAndReturnGeneratedKey
            )
        }
    }

    fun persisterOverstyringArbeidsforhold(
        hendelseId: UUID,
        fødselsnummer: String,
        orgnummer: String,
        begrunnelse: String,
        forklaring: String,
        deaktivert: Boolean,
        skjæringstidspunkt: LocalDate,
        saksbehandlerRef: UUID,
        tidspunkt: LocalDateTime
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery = """
                INSERT INTO overstyring_arbeidsforhold(hendelse_ref, person_ref, arbeidsgiver_ref, saksbehandler_ref, begrunnelse, forklaring, deaktivert, skjaeringstidspunkt, tidspunkt)
                SELECT :hendelse_id, p.id, ag.id, :saksbehandler_ref, :begrunnelse, :forklaring, :deaktivert, :skjaeringstidspunkt, :tidspunkt
                FROM arbeidsgiver ag,
                     person p
                WHERE p.fodselsnummer = :fodselsnummer
                  AND ag.orgnummer = :orgnr
            """.trimIndent()
            session.run(
                queryOf(
                    opprettOverstyringQuery,
                    mapOf(
                        "hendelse_id" to hendelseId,
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "orgnr" to orgnummer.toLong(),
                        "saksbehandler_ref" to saksbehandlerRef,
                        "begrunnelse" to begrunnelse,
                        "forklaring" to forklaring,
                        "deaktivert" to deaktivert,
                        "skjaeringstidspunkt" to skjæringstidspunkt,
                        "tidspunkt" to tidspunkt

                    )
                ).asUpdateAndReturnGeneratedKey
            )
        }
    }
}
