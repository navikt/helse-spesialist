package no.nav.helse.modell.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.overstyring.OverstyrArbeidsgiverDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.intellij.lang.annotations.Language

class OverstyringDao(private val dataSource: DataSource): HelseDao(dataSource) {

    fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID): List<OverstyringType> =
        """ SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN ot.id IS NOT NULL THEN 'Dager'
                END type
            FROM overstyring o
            LEFT JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
            LEFT JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
            LEFT JOIN overstyring_tidslinje ot on o.id = ot.overstyring_ref
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
            AND o.ferdigstilt = false
        """.list(mapOf("vedtaksperiode_id" to vedtaksperiodeId)) { OverstyringType.valueOf(it.string("type")) }

    fun finnAktiveOverstyringer(vedtaksperiodeId: UUID): List<EksternHendelseId> =
        """
            SELECT o.ekstern_hendelse_id FROM overstyring o
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
            AND o.ferdigstilt = false
        """.list(mapOf("vedtaksperiode_id" to vedtaksperiodeId)) { it.uuid("ekstern_hendelse_id") }

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

    fun kobleOverstyringOgVedtaksperiode(vedtaksperiodeIder: List<UUID>, overstyringHendelseId: UUID) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->

            @Language("PostgreSQL")
            val kobleOverstyringOgVedtaksperiodeQuery = """
                INSERT INTO overstyringer_for_vedtaksperioder(vedtaksperiode_id, overstyring_ref)
                SELECT :vedtaksperiode_id, o.id
                FROM overstyring o
                WHERE o.ekstern_hendelse_id = :overstyring_hendelse_id
                ON CONFLICT DO NOTHING
            """.trimIndent()

            session.transaction { transactionalSession ->
                vedtaksperiodeIder.forEach { vedtaksperiodeId ->
                    transactionalSession.run(
                        queryOf(
                            kobleOverstyringOgVedtaksperiodeQuery,
                            mapOf(
                                "vedtaksperiode_id" to vedtaksperiodeId,
                                "overstyring_hendelse_id" to overstyringHendelseId
                            )
                        ).asUpdate
                    )
                }
            }
        }
    }

    fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID): Boolean =
        """ SELECT 1 FROM overstyringer_for_vedtaksperioder ofv
            JOIN overstyring o ON o.id = ofv.overstyring_ref
            WHERE ofv.vedtaksperiode_id = :vedtaksperiode_id
            AND o.ferdigstilt = false
            LIMIT 1
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
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt)
                SELECT :hendelse_id, :ekstern_hendelse_id, p.id, :saksbehandler_ref, :tidspunkt
                FROM person p
                WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringTidslinjeQuery = """
                INSERT INTO overstyring_tidslinje(overstyring_ref, arbeidsgiver_ref, begrunnelse)
                SELECT :overstyring_ref, ag.id, :begrunnelse
                FROM arbeidsgiver ag
                WHERE ag.orgnummer = :orgnr
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringDagQuery = """
                INSERT INTO overstyring_dag(dato, dagtype, grad, fra_dagtype, fra_grad, overstyring_tidslinje_ref)
                VALUES (:dato, :dagtype, :grad, :fra_dagtype, :fra_grad, :overstyring_tidslinje_ref)
            """.trimIndent()

            session.transaction { transactionalSession ->
                val overstyringRef = transactionalSession.run(
                    queryOf(
                        opprettOverstyringQuery,
                        mapOf(
                            "hendelse_id" to hendelseId,
                            "ekstern_hendelse_id" to eksternHendelseId,
                            "fodselsnummer" to fødselsnummer.toLong(),
                            "saksbehandler_ref" to saksbehandlerRef,
                            "tidspunkt" to tidspunkt
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
                val overstyringTidslinjeRef = transactionalSession.run(
                    queryOf(
                        opprettOverstyringTidslinjeQuery,
                        mapOf(
                            "overstyring_ref" to overstyringRef,
                            "orgnr" to organisasjonsnummer.toLong(),
                            "begrunnelse" to begrunnelse,
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
                overstyrteDager.forEach { dag ->
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringDagQuery,
                            mapOf(
                                "dato" to dag.dato,
                                "dagtype" to dag.type.toString(),
                                "grad" to dag.grad,
                                "fra_dagtype" to dag.fraType.toString(),
                                "fra_grad" to dag.fraGrad,
                                "overstyring_tidslinje_ref" to overstyringTidslinjeRef
                            )
                        ).asUpdate
                    )
                }
            }
        }
    }

    fun persisterOverstyringInntektOgRefusjon(
        hendelseId: UUID,
        eksternHendelseId: UUID,
        fødselsnummer: String,
        arbeidsgivere: List<OverstyrArbeidsgiverDto>,
        saksbehandlerRef: UUID,
        skjæringstidspunkt: LocalDate,
        tidspunkt: LocalDateTime,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery = """
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt)
                SELECT :hendelse_id, :ekstern_hendelse_id, p.id, :saksbehandler_ref, :tidspunkt
                FROM person p
                WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringInntektOgRefusjonQuery = """
                INSERT INTO overstyring_inntekt(forklaring, manedlig_inntekt, fra_manedlig_inntekt, skjaeringstidspunkt, overstyring_ref, refusjonsopplysninger, fra_refusjonsopplysninger, begrunnelse, arbeidsgiver_ref, subsumsjon)
                SELECT :forklaring, :manedlig_inntekt, :fra_manedlig_inntekt, :skjaeringstidspunkt, :overstyring_ref, :refusjonsopplysninger::json, :fra_refusjonsopplysninger::json, :begrunnelse, ag.id, :subsumsjon::json
                FROM arbeidsgiver ag
                WHERE ag.orgnummer = :orgnr
            """.trimIndent()

            session.transaction { transactionalSession ->
                val overstyringRef = transactionalSession.run(
                    queryOf(
                        opprettOverstyringQuery,
                        mapOf(
                            "hendelse_id" to hendelseId,
                            "ekstern_hendelse_id" to eksternHendelseId,
                            "saksbehandler_ref" to saksbehandlerRef,
                            "tidspunkt" to tidspunkt,
                            "fodselsnummer" to fødselsnummer.toLong()
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
                arbeidsgivere.forEach { arbeidsgiver ->
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringInntektOgRefusjonQuery,
                            mapOf(
                                "forklaring" to arbeidsgiver.forklaring,
                                "manedlig_inntekt" to arbeidsgiver.månedligInntekt,
                                "fra_manedlig_inntekt" to arbeidsgiver.fraMånedligInntekt,
                                "skjaeringstidspunkt" to skjæringstidspunkt,
                                "overstyring_ref" to overstyringRef,
                                "refusjonsopplysninger" to arbeidsgiver.refusjonsopplysninger?.let { objectMapper.writeValueAsString(arbeidsgiver.refusjonsopplysninger) },
                                "fra_refusjonsopplysninger" to arbeidsgiver.fraRefusjonsopplysninger?.let { objectMapper.writeValueAsString(arbeidsgiver.fraRefusjonsopplysninger) },
                                "begrunnelse" to arbeidsgiver.begrunnelse,
                                "orgnr" to arbeidsgiver.organisasjonsnummer.toLong(),
                                "subsumsjon" to arbeidsgiver.subsumsjon?.let { objectMapper.writeValueAsString(arbeidsgiver.subsumsjon) }
                            )
                        ).asUpdate
                    )
                }
            }
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
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt)
                SELECT :hendelse_id, :ekstern_hendelse_id, p.id, :saksbehandler_ref, :tidspunkt
                FROM person p
                WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringArbeidsforholdQuery = """
                INSERT INTO overstyring_arbeidsforhold(forklaring, deaktivert, skjaeringstidspunkt, overstyring_ref, begrunnelse, arbeidsgiver_ref)
                SELECT :forklaring, :deaktivert, :skjaeringstidspunkt, :overstyring_ref, :begrunnelse, ag.id
                FROM arbeidsgiver ag
                WHERE ag.orgnummer = :orgnr
            """.trimIndent()

            val overstyringRef = session.run(
                queryOf(
                    opprettOverstyringQuery,
                    mapOf(
                        "hendelse_id" to hendelseId,
                        "ekstern_hendelse_id" to eksternHendelseId,
                        "saksbehandler_ref" to saksbehandlerRef,
                        "tidspunkt" to tidspunkt,
                        "fodselsnummer" to fødselsnummer.toLong()
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
                        "overstyring_ref" to overstyringRef,
                        "begrunnelse" to begrunnelse,
                        "orgnr" to organisasjonsnummer.toLong()
                    )
                ).asUpdate
            )
        }
    }
}

private typealias EksternHendelseId = UUID