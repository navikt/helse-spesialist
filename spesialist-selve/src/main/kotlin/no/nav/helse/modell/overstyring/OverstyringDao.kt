package no.nav.helse.modell.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.intellij.lang.annotations.Language

class OverstyringDao(private val dataSource: DataSource): HelseDao(dataSource) {

    fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID): List<OverstyringType> =
        """ SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN od.id IS NOT NULL THEN 'Dager'
                END type
            FROM overstyring o
            LEFT JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
            LEFT JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
            LEFT JOIN overstyring_dag od on o.id = od.overstyring_ref
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
            AND o.ferdigstilt = false
        """.list(mapOf("vedtaksperiode_id" to vedtaksperiodeId)) { OverstyringType.valueOf(it.string("type")) }

    fun ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId: UUID) =
        """ UPDATE overstyring
            SET ferdigstilt = true
            WHERE id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
        """.update(
            mapOf(
                "vedtaksperiode_id" to vedtaksperiodeId
            )
        )

    fun kobleOverstyringOgVedtaksperiode(vedtaksperiodeId: UUID, overstyringHendelseId: UUID) =
        """ INSERT INTO overstyringer_for_vedtaksperioder (vedtaksperiode_id, overstyring_ref)
            SELECT :vedtaksperiode_id, o.id
            FROM overstyring o
            WHERE o.ekstern_hendelse_id = :overstyring_hendelse_id
            ON CONFLICT DO NOTHING
        """.update(
            mapOf(
                "vedtaksperiode_id" to vedtaksperiodeId,
                "overstyring_hendelse_id" to overstyringHendelseId,
            )
        )

    fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID): Boolean =
        """ SELECT 1 FROM overstyringer_for_vedtaksperioder ofv
            JOIN overstyring o ON o.id = ofv.overstyring_ref
            WHERE ofv.vedtaksperiode_id = :vedtaksperiode_id
            AND o.ferdigstilt = false
        """.single(mapOf("vedtaksperiode_id" to vedtaksperiodeId)) { row -> row.boolean(1) } ?: false

    fun finnesEksternHendelseId(eksternHendelseId: UUID): Boolean =
        """ SELECT 1 from overstyring 
            WHERE ekstern_hendelse_id = :eksternHendelseId
        """.single(mapOf("eksternHendelseId" to eksternHendelseId)) { row -> row.boolean(1) } ?: false

    // Skal ikke kunne være null for fremtidige overstyringer. Vær obs hvis den skal brukes på eldre data.
    fun finnEksternHendelseIdFraHendelseId(hendelseId: UUID) = requireNotNull(
        """ SELECT ekstern_hendelse_id FROM overstyring o
            WHERE o.hendelse_ref = :hendelseId
        """.single(mapOf("hendelseId" to hendelseId)) { row -> row.uuid("ekstern_hendelse_id") })

    fun persisterOverstyringTidslinje(
        hendelseId: UUID,
        eksternHendelseId: UUID,
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
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, arbeidsgiver_ref, begrunnelse, saksbehandler_ref, tidspunkt)
                SELECT :hendelse_id, :ekstern_hendelse_id, p.id, ag.id, :begrunnelse, :saksbehandler_ref, :tidspunkt
                FROM arbeidsgiver ag,
                     person p
                WHERE p.fodselsnummer = :fodselsnummer
                  AND ag.orgnummer = :orgnr
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringDagQuery = """
                INSERT INTO overstyring_dag(overstyring_ref, dato, dagtype, grad)
                VALUES (:overstyring_ref, :dato, :dagtype, :grad)
            """.trimIndent()

            val overstyringRef = session.run(
                queryOf(
                    opprettOverstyringQuery,
                    mapOf(
                        "hendelse_id" to hendelseId,
                        "ekstern_hendelse_id" to eksternHendelseId,
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
        eksternHendelseId: UUID,
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
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, arbeidsgiver_ref, begrunnelse, saksbehandler_ref, tidspunkt)
                SELECT :hendelse_id, :ekstern_hendelse_id, p.id, ag.id, :begrunnelse, :saksbehandler_ref, :tidspunkt
                FROM arbeidsgiver ag,
                     person p
                WHERE p.fodselsnummer = :fodselsnummer
                  AND ag.orgnummer = :orgnr
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringInntektQuery = """
                INSERT INTO overstyring_inntekt(forklaring, manedlig_inntekt, skjaeringstidspunkt, overstyring_ref)
                VALUES (:forklaring, :manedlig_inntekt, :skjaeringstidspunkt, :overstyring_ref)
            """.trimIndent()

            val overstyringRef = session.run(
                queryOf(
                    opprettOverstyringQuery,
                    mapOf(
                        "hendelse_id" to hendelseId,
                        "ekstern_hendelse_id" to eksternHendelseId,
                        "begrunnelse" to begrunnelse,
                        "saksbehandler_ref" to saksbehandlerRef,
                        "tidspunkt" to tidspunkt,
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "orgnr" to organisasjonsnummer.toLong()
                    )
                ).asUpdateAndReturnGeneratedKey
            )

            session.run(
                queryOf(
                    opprettOverstyringInntektQuery,
                    mapOf(
                        "forklaring" to forklaring,
                        "manedlig_inntekt" to månedligInntekt,
                        "skjaeringstidspunkt" to skjæringstidspunkt,
                        "overstyring_ref" to overstyringRef
                    )
                ).asUpdate
            )
        }
    }

    fun persisterOverstyringArbeidsforhold(
        hendelseId: UUID,
        eksternHendelseId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
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
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, arbeidsgiver_ref, begrunnelse, saksbehandler_ref, tidspunkt)
                SELECT :hendelse_id, :ekstern_hendelse_id, p.id, ag.id, :begrunnelse, :saksbehandler_ref, :tidspunkt
                FROM arbeidsgiver ag,
                     person p
                WHERE p.fodselsnummer = :fodselsnummer
                  AND ag.orgnummer = :orgnr
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringArbeidsforholdQuery = """
                INSERT INTO overstyring_arbeidsforhold(forklaring, deaktivert, skjaeringstidspunkt, overstyring_ref)
                VALUES (:forklaring, :deaktivert, :skjaeringstidspunkt, :overstyring_ref)
            """.trimIndent()

            val overstyringRef = session.run(
                queryOf(
                    opprettOverstyringQuery,
                    mapOf(
                        "hendelse_id" to hendelseId,
                        "ekstern_hendelse_id" to eksternHendelseId,
                        "begrunnelse" to begrunnelse,
                        "saksbehandler_ref" to saksbehandlerRef,
                        "tidspunkt" to tidspunkt,
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "orgnr" to organisasjonsnummer.toLong()
                    )
                ).asUpdateAndReturnGeneratedKey
            )

            session.run(
                queryOf(
                    opprettOverstyringArbeidsforholdQuery,
                    mapOf(
                        "forklaring" to forklaring,
                        "deaktivert" to deaktivert,
                        "skjaeringstidspunkt" to skjæringstidspunkt,
                        "overstyring_ref" to overstyringRef
                    )
                ).asUpdate
            )
        }
    }
}
