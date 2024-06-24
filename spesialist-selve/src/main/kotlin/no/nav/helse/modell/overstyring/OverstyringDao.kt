package no.nav.helse.modell.overstyring

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.db.OverstyrtArbeidsforholdForDatabase
import no.nav.helse.db.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

class OverstyringDao(private val dataSource: DataSource) : HelseDao(dataSource) {
    fun finnOverstyringerMedTypeForVedtaksperioder(vedtaksperiodeIder: List<UUID>): List<OverstyringType> =
        asSQL(
            """ SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN ot.id IS NOT NULL THEN 'Dager'
                    WHEN ss.id IS NOT NULL THEN 'Sykepengegrunnlag'
                END as type
            FROM overstyring o
            LEFT JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
            LEFT JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
            LEFT JOIN overstyring_tidslinje ot on o.id = ot.overstyring_ref
            LEFT JOIN skjonnsfastsetting_sykepengegrunnlag ss on o.id = ss.overstyring_ref
            WHERE o.vedtaksperiode_id IN (${vedtaksperiodeIder.joinToString { "?" }})
            AND o.ferdigstilt = false
        """,
            *vedtaksperiodeIder.toTypedArray(),
        ).list { OverstyringType.valueOf(it.string("type")) }

    fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID): List<OverstyringType> =
        asSQL(
            """ SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN ot.id IS NOT NULL THEN 'Dager'
                    WHEN ss.id IS NOT NULL THEN 'Sykepengegrunnlag'
                END as type
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
        """,
            mapOf("vedtaksperiode_id" to vedtaksperiodeId),
        ).list { OverstyringType.valueOf(it.string("type")) }

    fun finnAktiveOverstyringer(vedtaksperiodeId: UUID): List<EksternHendelseId> =
        asSQL(
            """
            SELECT o.ekstern_hendelse_id FROM overstyring o
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
            AND o.ferdigstilt = false
        """,
            mapOf("vedtaksperiode_id" to vedtaksperiodeId),
        ).list { it.uuid("ekstern_hendelse_id") }

    fun ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId: UUID) =
        asSQL(
            """ UPDATE overstyring
            SET ferdigstilt = true
            WHERE id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
        """,
            mapOf("vedtaksperiode_id" to vedtaksperiodeId),
        ).update()

    fun kobleOverstyringOgVedtaksperiode(
        vedtaksperiodeIder: List<UUID>,
        overstyringHendelseId: UUID,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->

            @Language("PostgreSQL")
            val kobleOverstyringOgVedtaksperiodeQuery =
                """
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
                                "overstyring_hendelse_id" to overstyringHendelseId,
                            ),
                        ).asUpdate,
                    )
                }
            }
        }
    }

    fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID): Boolean =
        asSQL(
            """ SELECT 1 FROM overstyringer_for_vedtaksperioder ofv
            JOIN overstyring o ON o.id = ofv.overstyring_ref
            WHERE ofv.vedtaksperiode_id = :vedtaksperiode_id
            AND o.ferdigstilt = false
            LIMIT 1
        """,
            mapOf("vedtaksperiode_id" to vedtaksperiodeId),
        ).single { row -> row.boolean(1) } ?: false

    fun finnesEksternHendelseId(eksternHendelseId: UUID): Boolean =
        asSQL(
            """ SELECT 1 from overstyring 
            WHERE ekstern_hendelse_id = :eksternHendelseId
        """,
            mapOf("eksternHendelseId" to eksternHendelseId),
        ).single { row -> row.boolean(1) } ?: false

    internal fun persisterOverstyringTidslinje(
        overstyrtTidslinje: OverstyrtTidslinjeForDatabase,
        saksbehandlerOid: UUID,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery =
                """
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt, vedtaksperiode_id)
                SELECT gen_random_uuid(), :ekstern_hendelse_id, p.id, :saksbehandler_ref, :tidspunkt, :vedtaksperiode_id
                FROM person p
                WHERE p.fodselsnummer = :fodselsnummer
                """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringTidslinjeQuery =
                """
                INSERT INTO overstyring_tidslinje(overstyring_ref, arbeidsgiver_ref, begrunnelse)
                SELECT :overstyring_ref, ag.id, :begrunnelse
                FROM arbeidsgiver ag
                WHERE ag.orgnummer = :orgnr
                """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringDagQuery =
                """
                INSERT INTO overstyring_dag(dato, dagtype, grad, fra_dagtype, fra_grad, overstyring_tidslinje_ref)
                VALUES (:dato, :dagtype, :grad, :fra_dagtype, :fra_grad, :overstyring_tidslinje_ref)
                """.trimIndent()

            session.transaction { transactionalSession ->
                val overstyringRef =
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringQuery,
                            mapOf(
                                "ekstern_hendelse_id" to overstyrtTidslinje.id,
                                "fodselsnummer" to overstyrtTidslinje.fødselsnummer.toLong(),
                                "saksbehandler_ref" to saksbehandlerOid,
                                "tidspunkt" to overstyrtTidslinje.opprettet,
                                "vedtaksperiode_id" to overstyrtTidslinje.vedtaksperiodeId,
                            ),
                        ).asUpdateAndReturnGeneratedKey,
                    )
                val overstyringTidslinjeRef =
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringTidslinjeQuery,
                            mapOf(
                                "overstyring_ref" to overstyringRef,
                                "orgnr" to overstyrtTidslinje.organisasjonsnummer.toLong(),
                                "begrunnelse" to overstyrtTidslinje.begrunnelse,
                            ),
                        ).asUpdateAndReturnGeneratedKey,
                    )
                overstyrtTidslinje.dager.forEach { dag ->
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringDagQuery,
                            mapOf(
                                "dato" to dag.dato,
                                "dagtype" to dag.type,
                                "grad" to dag.grad,
                                "fra_dagtype" to dag.fraType,
                                "fra_grad" to dag.fraGrad,
                                "overstyring_tidslinje_ref" to overstyringTidslinjeRef,
                            ),
                        ).asUpdate,
                    )
                }
            }
        }
    }

    internal fun persisterOverstyringInntektOgRefusjon(
        overstyrtInntektOgRefusjon: OverstyrtInntektOgRefusjonForDatabase,
        saksbehandlerOid: UUID,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery =
                """
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt, vedtaksperiode_id)
                SELECT gen_random_uuid(), :ekstern_hendelse_id, p.id, :saksbehandler_ref, :tidspunkt, :vedtaksperiode_id
                FROM person p
                WHERE p.fodselsnummer = :fodselsnummer
                """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringInntektOgRefusjonQuery =
                """
                INSERT INTO overstyring_inntekt(forklaring, manedlig_inntekt, fra_manedlig_inntekt, skjaeringstidspunkt, overstyring_ref, refusjonsopplysninger, fra_refusjonsopplysninger, begrunnelse, arbeidsgiver_ref, subsumsjon)
                SELECT :forklaring, :manedlig_inntekt, :fra_manedlig_inntekt, :skjaeringstidspunkt, :overstyring_ref, :refusjonsopplysninger::json, :fra_refusjonsopplysninger::json, :begrunnelse, ag.id, :subsumsjon::json
                FROM arbeidsgiver ag
                WHERE ag.orgnummer = :orgnr
                """.trimIndent()

            session.transaction { transactionalSession ->
                val overstyringRef =
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringQuery,
                            mapOf(
                                "ekstern_hendelse_id" to overstyrtInntektOgRefusjon.id,
                                "fodselsnummer" to overstyrtInntektOgRefusjon.fødselsnummer.toLong(),
                                "saksbehandler_ref" to saksbehandlerOid,
                                "tidspunkt" to overstyrtInntektOgRefusjon.opprettet,
                                "vedtaksperiode_id" to overstyrtInntektOgRefusjon.vedtaksperiodeId,
                            ),
                        ).asUpdateAndReturnGeneratedKey,
                    )
                overstyrtInntektOgRefusjon.arbeidsgivere.forEach { arbeidsgiver ->
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringInntektOgRefusjonQuery,
                            mapOf(
                                "forklaring" to arbeidsgiver.forklaring,
                                "manedlig_inntekt" to arbeidsgiver.månedligInntekt,
                                "fra_manedlig_inntekt" to arbeidsgiver.fraMånedligInntekt,
                                "skjaeringstidspunkt" to overstyrtInntektOgRefusjon.skjæringstidspunkt,
                                "overstyring_ref" to overstyringRef,
                                "refusjonsopplysninger" to
                                    arbeidsgiver.refusjonsopplysninger?.let {
                                        objectMapper.writeValueAsString(
                                            arbeidsgiver.refusjonsopplysninger,
                                        )
                                    },
                                "fra_refusjonsopplysninger" to
                                    arbeidsgiver.fraRefusjonsopplysninger?.let {
                                        objectMapper.writeValueAsString(
                                            arbeidsgiver.fraRefusjonsopplysninger,
                                        )
                                    },
                                "begrunnelse" to arbeidsgiver.begrunnelse,
                                "orgnr" to arbeidsgiver.organisasjonsnummer.toLong(),
                                "subsumsjon" to
                                    arbeidsgiver.lovhjemmel?.let {
                                        objectMapper.writeValueAsString(
                                            arbeidsgiver.lovhjemmel,
                                        )
                                    },
                            ),
                        ).asUpdate,
                    )
                }
            }
        }
    }

    internal fun persisterSkjønnsfastsettingSykepengegrunnlag(
        skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlagForDatabase,
        saksbehandlerOid: UUID,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery =
                """
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt, vedtaksperiode_id)
                SELECT gen_random_uuid(), :ekstern_hendelse_id, p.id, :saksbehandler_ref, :tidspunkt, :vedtaksperiode_id
                FROM person p
                WHERE p.fodselsnummer = :fodselsnummer
                """.trimIndent()

            @Language("PostgreSQL")
            val opprettBegrunnelseQuery =
                """
                INSERT INTO begrunnelse(tekst, type, saksbehandler_ref) VALUES (:tekst, :type, :saksbehandler_ref)
                """.trimIndent()

            @Language("PostgreSQL")
            val opprettSkjønnsfastsettingSykepengegrunnlagQuery =
                """
                INSERT INTO skjonnsfastsetting_sykepengegrunnlag(skjaeringstidspunkt, arsak, subsumsjon, overstyring_ref, initierende_vedtaksperiode_id, begrunnelse_fritekst_ref, begrunnelse_mal_ref, begrunnelse_konklusjon_ref, type)
                VALUES (:skjaeringstidspunkt, :arsak, :subsumsjon::json, :overstyring_ref, :initierende_vedtaksperiode_id, :begrunnelse_fritekst_ref, :begrunnelse_mal_ref, :begrunnelse_konklusjon_ref, :type)
                """.trimIndent()

            @Language("PostgreSQL")
            val opprettSkjønnsfastsettingSykepengegrunnlagArbeidsgiverQuery =
                """
                INSERT INTO skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver(arlig, fra_arlig, arbeidsgiver_ref, skjonnsfastsetting_sykepengegrunnlag_ref)
                SELECT :arlig, :fra_arlig, ag.id, :skjonnsfastsetting_sykepengegrunnlag_ref
                FROM arbeidsgiver ag
                WHERE ag.orgnummer = :orgnr
                """.trimIndent()

            session.transaction { transactionalSession ->
                val overstyringRef =
                    transactionalSession.run(
                        queryOf(
                            opprettOverstyringQuery,
                            mapOf(
                                "ekstern_hendelse_id" to skjønnsfastsattSykepengegrunnlag.id,
                                "saksbehandler_ref" to saksbehandlerOid,
                                "tidspunkt" to skjønnsfastsattSykepengegrunnlag.opprettet,
                                "fodselsnummer" to skjønnsfastsattSykepengegrunnlag.fødselsnummer.toLong(),
                                "vedtaksperiode_id" to skjønnsfastsattSykepengegrunnlag.vedtaksperiodeId,
                            ),
                        ).asUpdateAndReturnGeneratedKey,
                    )
                // Den felles informasjonen ligger på alle arbeidsgiverne. Burde kanskje skilles ut i eget objekt
                val enArbeidsgiver = skjønnsfastsattSykepengegrunnlag.arbeidsgivere.first()
                val begrunnelseFritekstId =
                    requireNotNull(
                        transactionalSession.run(
                            queryOf(
                                opprettBegrunnelseQuery,
                                mapOf(
                                    "tekst" to enArbeidsgiver.begrunnelseFritekst,
                                    "type" to "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_FRITEKST",
                                    "saksbehandler_ref" to saksbehandlerOid,
                                ),
                            ).asUpdateAndReturnGeneratedKey,
                        ),
                    ) { "Forventer å kunne opprette begrunnelseFritekst" }
                val begrunnelseMalId =
                    requireNotNull(
                        transactionalSession.run(
                            queryOf(
                                opprettBegrunnelseQuery,
                                mapOf(
                                    "tekst" to enArbeidsgiver.begrunnelseMal,
                                    "type" to "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_MAL",
                                    "saksbehandler_ref" to saksbehandlerOid,
                                ),
                            ).asUpdateAndReturnGeneratedKey,
                        ),
                    ) { "Forventer å kunne opprette begrunnelseMal" }
                val begrunnelseKonklusjonId =
                    requireNotNull(
                        transactionalSession.run(
                            queryOf(
                                opprettBegrunnelseQuery,
                                mapOf(
                                    "tekst" to enArbeidsgiver.begrunnelseKonklusjon,
                                    "type" to "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_KONKLUSJON",
                                    "saksbehandler_ref" to saksbehandlerOid,
                                ),
                            ).asUpdateAndReturnGeneratedKey,
                        ),
                    ) { "Forventer å kunne opprette begrunnelseMal" }
                val skjønnsfastsettingSykepengegrunnlagId =
                    requireNotNull(
                        transactionalSession.run(
                            queryOf(
                                opprettSkjønnsfastsettingSykepengegrunnlagQuery,
                                mapOf(
                                    "skjaeringstidspunkt" to skjønnsfastsattSykepengegrunnlag.skjæringstidspunkt,
                                    "arsak" to enArbeidsgiver.årsak,
                                    "type" to enArbeidsgiver.type.name,
                                    "subsumsjon" to
                                        enArbeidsgiver.lovhjemmel?.let {
                                            objectMapper.writeValueAsString(
                                                enArbeidsgiver.lovhjemmel,
                                            )
                                        },
                                    "overstyring_ref" to overstyringRef,
                                    "initierende_vedtaksperiode_id" to enArbeidsgiver.initierendeVedtaksperiodeId?.let { UUID.fromString(it) },
                                    "begrunnelse_fritekst_ref" to begrunnelseFritekstId,
                                    "begrunnelse_mal_ref" to begrunnelseMalId,
                                    "begrunnelse_konklusjon_ref" to begrunnelseKonklusjonId,
                                ),
                            ).asUpdateAndReturnGeneratedKey,
                        ),
                    )
                skjønnsfastsattSykepengegrunnlag.arbeidsgivere.forEach { arbeidsgiver ->
                    transactionalSession.run(
                        queryOf(
                            opprettSkjønnsfastsettingSykepengegrunnlagArbeidsgiverQuery,
                            mapOf(
                                "arlig" to arbeidsgiver.årlig,
                                "fra_arlig" to arbeidsgiver.fraÅrlig,
                                "orgnr" to arbeidsgiver.organisasjonsnummer.toLong(),
                                "skjonnsfastsetting_sykepengegrunnlag_ref" to skjønnsfastsettingSykepengegrunnlagId,
                            ),
                        ).asUpdate,
                    )
                }
            }
        }
    }

    fun persisterOverstyringArbeidsforhold(
        overstyrtArbeidsforhold: OverstyrtArbeidsforholdForDatabase,
        saksbehandlerOid: UUID,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val opprettOverstyringQuery =
                """
                INSERT INTO overstyring(hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt, vedtaksperiode_id)
                SELECT gen_random_uuid(), :ekstern_hendelse_id, p.id, :saksbehandler_ref, :tidspunkt, :vedtaksperiode_id
                FROM person p
                WHERE p.fodselsnummer = :fodselsnummer
                """.trimIndent()

            @Language("PostgreSQL")
            val opprettOverstyringArbeidsforholdQuery =
                """
                INSERT INTO overstyring_arbeidsforhold(forklaring, deaktivert, skjaeringstidspunkt, overstyring_ref, begrunnelse, arbeidsgiver_ref)
                SELECT :forklaring, :deaktivert, :skjaeringstidspunkt, :overstyring_ref, :begrunnelse, ag.id
                FROM arbeidsgiver ag
                WHERE ag.orgnummer = :orgnr
                """.trimIndent()

            val overstyringRef =
                session.run(
                    queryOf(
                        opprettOverstyringQuery,
                        mapOf(
                            "ekstern_hendelse_id" to overstyrtArbeidsforhold.id,
                            "fodselsnummer" to overstyrtArbeidsforhold.fødselsnummer.toLong(),
                            "saksbehandler_ref" to saksbehandlerOid,
                            "tidspunkt" to overstyrtArbeidsforhold.opprettet,
                            "vedtaksperiode_id" to overstyrtArbeidsforhold.vedtaksperiodeId,
                        ),
                    ).asUpdateAndReturnGeneratedKey,
                )

            overstyrtArbeidsforhold.overstyrteArbeidsforhold.forEach { arbeidsforhold ->
                session.run(
                    queryOf(
                        opprettOverstyringArbeidsforholdQuery,
                        mapOf(
                            "forklaring" to arbeidsforhold.forklaring,
                            "deaktivert" to arbeidsforhold.deaktivert,
                            "skjaeringstidspunkt" to overstyrtArbeidsforhold.skjæringstidspunkt,
                            "overstyring_ref" to overstyringRef,
                            "begrunnelse" to arbeidsforhold.begrunnelse,
                            "orgnr" to arbeidsforhold.organisasjonsnummer.toLong(),
                        ),
                    ).asUpdate,
                )
            }
        }
    }
}

private typealias EksternHendelseId = UUID
