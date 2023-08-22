package no.nav.helse.modell.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.intellij.lang.annotations.Language

class OverstyringDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID): List<OverstyringType> = asSQL(
        """ SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN ot.id IS NOT NULL THEN 'Dager'
                    WHEN ss.id IS NOT NULL THEN 'Sykepengegrunnlag'
                END type
            FROM overstyring o
            LEFT JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
            LEFT JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
            LEFT JOIN overstyring_tidslinje ot on o.id = ot.overstyring_ref
            LEFT JOIN skjonnsfastsetting_sykepengegrunnlag ss on o.id = ss.overstyring_ref
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
            AND o.ferdigstilt = false
        """, mapOf("vedtaksperiode_id" to vedtaksperiodeId)
    ).list { OverstyringType.valueOf(it.string("type")) }

    fun finnAktiveOverstyringer(vedtaksperiodeId: UUID): List<EksternHendelseId> = asSQL(
        """
            SELECT o.ekstern_hendelse_id FROM overstyring o
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
            AND o.ferdigstilt = false
        """, mapOf("vedtaksperiode_id" to vedtaksperiodeId)
    ).list { it.uuid("ekstern_hendelse_id") }

    fun ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId: UUID) = asSQL(
        """ UPDATE overstyring
            SET ferdigstilt = true
            WHERE id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
        """, mapOf("vedtaksperiode_id" to vedtaksperiodeId)
    ).update()

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

    fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID): Boolean = asSQL(
        """ SELECT 1 FROM overstyringer_for_vedtaksperioder ofv
            JOIN overstyring o ON o.id = ofv.overstyring_ref
            WHERE ofv.vedtaksperiode_id = :vedtaksperiode_id
            AND o.ferdigstilt = false
            LIMIT 1
        """, mapOf("vedtaksperiode_id" to vedtaksperiodeId)
    ).single { row -> row.boolean(1) } ?: false

    fun finnesEksternHendelseId(eksternHendelseId: UUID): Boolean = asSQL(
        """ SELECT 1 from overstyring 
            WHERE ekstern_hendelse_id = :eksternHendelseId
        """, mapOf("eksternHendelseId" to eksternHendelseId)
    ).single { row -> row.boolean(1) } ?: false

    // Skal ikke kunne være null for fremtidige overstyringer. Vær obs hvis den skal brukes på eldre data.
    fun finnEksternHendelseIdFraHendelseId(hendelseId: UUID) = requireNotNull(asSQL(
        """ SELECT ekstern_hendelse_id FROM overstyring o
            WHERE o.hendelse_ref = :hendelseId
        """, mapOf("hendelseId" to hendelseId)
    ).single { row -> row.uuid("ekstern_hendelse_id") })

    fun persisterOverstyringTidslinje(
        hendelseId: UUID,
        eksternHendelseId: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        saksbehandlerRef: UUID,
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

    internal fun persisterOverstyringInntektOgRefusjon(
        hendelseId: UUID,
        eksternHendelseId: UUID,
        fødselsnummer: String,
        arbeidsgivere: List<OverstyrtArbeidsgiver>,
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
                                "refusjonsopplysninger" to arbeidsgiver.refusjonsopplysninger?.let {
                                    objectMapper.writeValueAsString(
                                        arbeidsgiver.refusjonsopplysninger
                                    )
                                },
                                "fra_refusjonsopplysninger" to arbeidsgiver.fraRefusjonsopplysninger?.let {
                                    objectMapper.writeValueAsString(
                                        arbeidsgiver.fraRefusjonsopplysninger
                                    )
                                },
                                "begrunnelse" to arbeidsgiver.begrunnelse,
                                "orgnr" to arbeidsgiver.organisasjonsnummer.toLong(),
                                "subsumsjon" to arbeidsgiver.subsumsjon?.let {
                                    objectMapper.writeValueAsString(
                                        arbeidsgiver.subsumsjon
                                    )
                                }
                            )
                        ).asUpdate
                    )
                }
            }
        }
    }

    internal fun persisterSkjønnsfastsettingSykepengegrunnlag(
        hendelseId: UUID,
        eksternHendelseId: UUID,
        fødselsnummer: String,
        arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
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
            """

            @Language("PostgreSQL")
            val opprettBegrunnelseQuery = """
                INSERT INTO begrunnelse(tekst, type, saksbehandler_ref) VALUES (:tekst, :type, :saksbehandler_ref)
            """

            @Language("PostgreSQL")
            val opprettSkjønnsfastsettingSykepengegrunnlagQuery = """
                INSERT INTO skjonnsfastsetting_sykepengegrunnlag(skjaeringstidspunkt, arsak, subsumsjon, overstyring_ref, initierende_vedtaksperiode_id, begrunnelse_fritekst_ref, begrunnelse_mal_ref, begrunnelse_konklusjon_ref, type)
                VALUES (:skjaeringstidspunkt, :arsak, :subsumsjon::json, :overstyring_ref, :initierende_vedtaksperiode_id, :begrunnelse_fritekst_ref, :begrunnelse_mal_ref, :begrunnelse_konklusjon_ref, :type)
            """

            @Language("PostgreSQL")
            val opprettSkjønnsfastsettingSykepengegrunnlagArbeidsgiverQuery = """
                INSERT INTO skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver(arlig, fra_arlig, arbeidsgiver_ref, skjonnsfastsetting_sykepengegrunnlag_ref)
                SELECT :arlig, :fra_arlig, ag.id, :skjonnsfastsetting_sykepengegrunnlag_ref
                FROM arbeidsgiver ag
                WHERE ag.orgnummer = :orgnr
            """

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
                val begrunnelseFritekstId = requireNotNull(transactionalSession.run(
                    queryOf(
                        opprettBegrunnelseQuery,
                        mapOf(
                            "tekst" to arbeidsgivere.first().begrunnelseFritekst,
                            "type" to "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_FRITEKST",
                            "saksbehandler_ref" to saksbehandlerRef
                        )
                    ).asUpdateAndReturnGeneratedKey
                )) { "Forventer å kunne opprette begrunnelseFritekst" }
                val begrunnelseMalId = requireNotNull(transactionalSession.run(
                    queryOf(
                        opprettBegrunnelseQuery,
                        mapOf(
                            "tekst" to arbeidsgivere.first().begrunnelseMal,
                            "type" to "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_MAL",
                            "saksbehandler_ref" to saksbehandlerRef
                        )
                    ).asUpdateAndReturnGeneratedKey
                )) { "Forventer å kunne opprette begrunnelseMal" }
                val begrunnelseKonklusjonId = requireNotNull(transactionalSession.run(
                    queryOf(
                        opprettBegrunnelseQuery,
                        mapOf(
                            "tekst" to arbeidsgivere.first().begrunnelseKonklusjon,
                            "type" to "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_KONKLUSJON",
                            "saksbehandler_ref" to saksbehandlerRef
                        )
                    ).asUpdateAndReturnGeneratedKey
                )) { "Forventer å kunne opprette begrunnelseMal" }
                val skjønnsfastsettingSykepengegrunnlagId = requireNotNull(transactionalSession.run(
                    queryOf(
                        opprettSkjønnsfastsettingSykepengegrunnlagQuery,
                        mapOf(
                            "skjaeringstidspunkt" to skjæringstidspunkt,
                            "arsak" to arbeidsgivere.first().årsak,
                            "type" to arbeidsgivere.first().type.name,
                            "subsumsjon" to arbeidsgivere.first().subsumsjon?.let {
                                objectMapper.writeValueAsString(
                                    arbeidsgivere.first().subsumsjon
                                )
                            },
                            "overstyring_ref" to overstyringRef,
                            "initierende_vedtaksperiode_id" to arbeidsgivere.first().initierendeVedtaksperiodeId,
                            "begrunnelse_fritekst_ref" to begrunnelseFritekstId,
                            "begrunnelse_mal_ref" to begrunnelseMalId,
                            "begrunnelse_konklusjon_ref" to begrunnelseKonklusjonId,
                        )
                    ).asUpdateAndReturnGeneratedKey
                ))
                arbeidsgivere.forEach { arbeidsgiver ->
                    transactionalSession.run(
                        queryOf(
                            opprettSkjønnsfastsettingSykepengegrunnlagArbeidsgiverQuery,
                            mapOf(
                                "arlig" to arbeidsgiver.årlig,
                                "fra_arlig" to arbeidsgiver.fraÅrlig,
                                "orgnr" to arbeidsgiver.organisasjonsnummer.toLong(),
                                "skjonnsfastsetting_sykepengegrunnlag_ref" to skjønnsfastsettingSykepengegrunnlagId,
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
