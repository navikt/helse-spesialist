package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.asSQLWithQuestionMarks
import no.nav.helse.db.overstyring.MinimumSykdomsgradForDatabase
import no.nav.helse.db.overstyring.OverstyringForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import java.util.UUID
import javax.sql.DataSource

class PgOverstyringDao private constructor(queryRunner: QueryRunner) : OverstyringDao, QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun finnOverstyringerMedTypeForVedtaksperioder(vedtaksperiodeIder: List<UUID>) =
        asSQLWithQuestionMarks(
            """
            SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN ot.id IS NOT NULL THEN 'Dager'
                    WHEN ss.id IS NOT NULL THEN 'Sykepengegrunnlag'
                    WHEN oms.id IS NOT NULL THEN 'MinimumSykdomsgrad'
                END as type
            FROM overstyring o
            LEFT JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
            LEFT JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
            LEFT JOIN overstyring_tidslinje ot on o.id = ot.overstyring_ref
            LEFT JOIN skjonnsfastsetting_sykepengegrunnlag ss on o.id = ss.overstyring_ref
            LEFT JOIN overstyring_minimum_sykdomsgrad oms on o.id = oms.overstyring_ref
            WHERE o.vedtaksperiode_id = ANY (?)
            AND o.ferdigstilt = false
            """.trimIndent(),
            vedtaksperiodeIder.toTypedArray(),
        ).list { OverstyringType.valueOf(it.string("type")) }

    override fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN ot.id IS NOT NULL THEN 'Dager'
                    WHEN ss.id IS NOT NULL THEN 'Sykepengegrunnlag'
                    WHEN oms.id IS NOT NULL THEN 'MinimumSykdomsgrad'
                END as type
            FROM overstyring o
            LEFT JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
            LEFT JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
            LEFT JOIN overstyring_tidslinje ot on o.id = ot.overstyring_ref
            LEFT JOIN skjonnsfastsetting_sykepengegrunnlag ss on o.id = ss.overstyring_ref
            LEFT JOIN overstyring_minimum_sykdomsgrad oms on o.id = oms.overstyring_ref
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiodeId
            )
            AND o.ferdigstilt = false
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).list { OverstyringType.valueOf(it.string("type")) }

    override fun finnAktiveOverstyringer(vedtaksperiodeId: UUID): List<EksternHendelseId> =
        asSQL(
            """
            SELECT o.ekstern_hendelse_id FROM overstyring o
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
            AND o.ferdigstilt = false
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).list { it.uuid("ekstern_hendelse_id") }

    override fun ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId: UUID) =
        asSQL(
            """
            UPDATE overstyring
            SET ferdigstilt = true
            WHERE id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).update()

    override fun kobleOverstyringOgVedtaksperiode(
        vedtaksperiodeIder: List<UUID>,
        overstyringHendelseId: UUID,
    ) {
        vedtaksperiodeIder.forEach { vedtaksperiode ->
            asSQL(
                """
                INSERT INTO overstyringer_for_vedtaksperioder (vedtaksperiode_id, overstyring_ref)
                SELECT :vedtaksperiodeId, o.id
                FROM overstyring o
                WHERE o.ekstern_hendelse_id = :overstyringHendelseId
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiode,
                "overstyringHendelseId" to overstyringHendelseId,
            ).update()
        }
    }

    override fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT 1
            FROM overstyringer_for_vedtaksperioder ofv
            JOIN overstyring o ON o.id = ofv.overstyring_ref
            WHERE ofv.vedtaksperiode_id = :vedtaksperiodeId
              AND o.ferdigstilt = false
            LIMIT 1
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { true } ?: false

    override fun finnesEksternHendelseId(eksternHendelseId: UUID) =
        asSQL(
            """
            SELECT 1 from overstyring
            WHERE ekstern_hendelse_id = :eksternHendelseId
            """.trimIndent(),
            "eksternHendelseId" to eksternHendelseId,
        ).singleOrNull { true } ?: false

    override fun persisterOverstyringTidslinje(
        overstyrtTidslinje: OverstyrtTidslinjeForDatabase,
        saksbehandlerOid: UUID,
    ) {
        val overstyringRef = insertIntoOverstyring(overstyrtTidslinje, saksbehandlerOid)
        val overstyringTidslinjeRef =
            asSQL(
                """
                INSERT INTO overstyring_tidslinje (overstyring_ref, arbeidsgiver_ref, begrunnelse)
                SELECT :overstyringRef, ag.id, :begrunnelse
                FROM arbeidsgiver ag
                WHERE ag.organisasjonsnummer = :organisasjonsnummer
                """.trimIndent(),
                "overstyringRef" to overstyringRef,
                "organisasjonsnummer" to overstyrtTidslinje.organisasjonsnummer,
                "begrunnelse" to overstyrtTidslinje.begrunnelse,
            ).updateAndReturnGeneratedKey()

        overstyrtTidslinje.dager.forEach { dag ->
            asSQL(
                """
                INSERT INTO overstyring_dag (dato, dagtype, grad, fra_dagtype, fra_grad, overstyring_tidslinje_ref)
                VALUES (:dato, :dagtype, :grad, :fraDagtype, :fraGrad, :overstyringTidslinjeRef)
                """.trimIndent(),
                "dato" to dag.dato,
                "dagtype" to dag.type,
                "grad" to dag.grad,
                "fraDagtype" to dag.fraType,
                "fraGrad" to dag.fraGrad,
                "overstyringTidslinjeRef" to overstyringTidslinjeRef,
            ).update()
        }
    }

    override fun persisterOverstyringInntektOgRefusjon(
        overstyrtInntektOgRefusjon: OverstyrtInntektOgRefusjonForDatabase,
        saksbehandlerOid: UUID,
    ) {
        val overstyringRef = insertIntoOverstyring(overstyrtInntektOgRefusjon, saksbehandlerOid)
        overstyrtInntektOgRefusjon.arbeidsgivere.forEach { arbeidsgiver ->
            asSQL(
                """
                INSERT INTO overstyring_inntekt (forklaring, manedlig_inntekt, fra_manedlig_inntekt, skjaeringstidspunkt, overstyring_ref, refusjonsopplysninger, fra_refusjonsopplysninger, begrunnelse, arbeidsgiver_ref, subsumsjon, fom, tom)
                SELECT :forklaring, :maanedligInntekt, :fraMaanedligInntekt, :skjaeringstidspunkt, :overstyringRef, :refusjonsopplysninger::json, :fraRefusjonsopplysninger::json, :begrunnelse, ag.id, :subsumsjon::json, :fom, :tom
                FROM arbeidsgiver ag
                WHERE ag.organisasjonsnummer = :organisasjonsnummer
                """.trimIndent(),
                "forklaring" to arbeidsgiver.forklaring,
                "maanedligInntekt" to arbeidsgiver.månedligInntekt,
                "fraMaanedligInntekt" to arbeidsgiver.fraMånedligInntekt,
                "skjaeringstidspunkt" to overstyrtInntektOgRefusjon.skjæringstidspunkt,
                "overstyringRef" to overstyringRef,
                "refusjonsopplysninger" to
                    arbeidsgiver.refusjonsopplysninger?.let {
                        objectMapper.writeValueAsString(
                            arbeidsgiver.refusjonsopplysninger,
                        )
                    },
                "fraRefusjonsopplysninger" to
                    arbeidsgiver.fraRefusjonsopplysninger?.let {
                        objectMapper.writeValueAsString(
                            arbeidsgiver.fraRefusjonsopplysninger,
                        )
                    },
                "begrunnelse" to arbeidsgiver.begrunnelse,
                "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
                "subsumsjon" to
                    arbeidsgiver.lovhjemmel?.let {
                        objectMapper.writeValueAsString(
                            arbeidsgiver.lovhjemmel,
                        )
                    },
                "fom" to arbeidsgiver.fom,
                "tom" to arbeidsgiver.tom,
            ).update()
        }
    }

    override fun persisterSkjønnsfastsettingSykepengegrunnlag(
        skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlagForDatabase,
        saksbehandlerOid: UUID,
    ) {
        // Den felles informasjonen ligger på alle arbeidsgiverne. Burde kanskje skilles ut i eget objekt
        val enArbeidsgiver = skjønnsfastsattSykepengegrunnlag.arbeidsgivere.first()

        val (begrunnelseFritekstId, begrunnelseMalId, begrunnelseKonklusjonId) =
            mapOf(
                "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_FRITEKST" to enArbeidsgiver.begrunnelseFritekst,
                "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_MAL" to enArbeidsgiver.begrunnelseMal,
                "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_KONKLUSJON" to enArbeidsgiver.begrunnelseKonklusjon,
            ).map { (type, tekst) ->
                checkNotNull(tekst)
                insertIntoBegrunnelse(type, tekst, saksbehandlerOid)
            }

        val overstyringRef = insertIntoOverstyring(skjønnsfastsattSykepengegrunnlag, saksbehandlerOid)
        val skjønnsfastsettingSykepengegrunnlagId =
            asSQL(
                """
                INSERT INTO skjonnsfastsetting_sykepengegrunnlag (skjaeringstidspunkt, arsak, subsumsjon, overstyring_ref, initierende_vedtaksperiode_id, begrunnelse_fritekst_ref, begrunnelse_mal_ref, begrunnelse_konklusjon_ref, type)
                VALUES (:skjaeringstidspunkt, :aarsak, :subsumsjon::json, :overstyringRef, :initierendeVedtaksperiodeId, :begrunnelseFritekstRef, :begrunnelseMalRef, :begrunnelseKonklusjonRef, :type)
                """.trimIndent(),
                "skjaeringstidspunkt" to skjønnsfastsattSykepengegrunnlag.skjæringstidspunkt,
                "aarsak" to enArbeidsgiver.årsak,
                "type" to enArbeidsgiver.type.name,
                "subsumsjon" to
                    enArbeidsgiver.lovhjemmel?.let { objectMapper.writeValueAsString(enArbeidsgiver.lovhjemmel) },
                "overstyringRef" to overstyringRef,
                "initierendeVedtaksperiodeId" to
                    enArbeidsgiver.initierendeVedtaksperiodeId?.let { UUID.fromString(it) },
                "begrunnelseFritekstRef" to begrunnelseFritekstId,
                "begrunnelseMalRef" to begrunnelseMalId,
                "begrunnelseKonklusjonRef" to begrunnelseKonklusjonId,
            ).updateAndReturnGeneratedKey()
        skjønnsfastsattSykepengegrunnlag.arbeidsgivere.forEach { arbeidsgiver ->
            asSQL(
                """
                INSERT INTO skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver (arlig, fra_arlig, arbeidsgiver_ref, skjonnsfastsetting_sykepengegrunnlag_ref)
                SELECT :aarlig, :fraAarlig, ag.id, :skjoennsfastsettingSykepengegrunnlagRef
                FROM arbeidsgiver ag
                WHERE ag.organisasjonsnummer = :organisasjonsnummer
                """.trimIndent(),
                "aarlig" to arbeidsgiver.årlig,
                "fraAarlig" to arbeidsgiver.fraÅrlig,
                "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
                "skjoennsfastsettingSykepengegrunnlagRef" to skjønnsfastsettingSykepengegrunnlagId,
            ).update()
        }
    }

    private fun insertIntoBegrunnelse(
        type: String,
        tekst: String,
        saksbehandlerOid: UUID,
    ) = asSQL(
        """
        INSERT INTO begrunnelse (type, tekst, saksbehandler_ref)
        VALUES (:type, :tekst, :saksbehandlerRef)
        """.trimIndent(),
        "type" to type,
        "tekst" to tekst,
        "saksbehandlerRef" to saksbehandlerOid,
    ).updateAndReturnGeneratedKey()

    override fun persisterMinimumSykdomsgrad(
        minimumSykdomsgrad: MinimumSykdomsgradForDatabase,
        saksbehandlerOid: UUID,
    ) {
        val overstyringId = insertIntoOverstyring(minimumSykdomsgrad, saksbehandlerOid)
        val overstyringMinimumSykdomsgradId =
            asSQL(
                """
                INSERT INTO overstyring_minimum_sykdomsgrad (overstyring_ref, fom, tom, vurdering, begrunnelse)
                VALUES (:overstyringRef, :fom, :tom, :vurdering, :begrunnelse)
                """.trimIndent(),
                "overstyringRef" to overstyringId,
                "fom" to (minimumSykdomsgrad.perioderVurdertOk.firstOrNull()?.fom ?: minimumSykdomsgrad.perioderVurdertIkkeOk.first().fom),
                "tom" to (minimumSykdomsgrad.perioderVurdertOk.firstOrNull()?.tom ?: minimumSykdomsgrad.perioderVurdertIkkeOk.first().tom),
                "vurdering" to (minimumSykdomsgrad.perioderVurdertOk.isNotEmpty()),
                "begrunnelse" to minimumSykdomsgrad.begrunnelse,
            ).updateAndReturnGeneratedKey()

        insertOverstyrtMinimumSykdomsgradPerioder(minimumSykdomsgrad.perioderVurdertOk, true, overstyringMinimumSykdomsgradId)
        insertOverstyrtMinimumSykdomsgradPerioder(minimumSykdomsgrad.perioderVurdertIkkeOk, false, overstyringMinimumSykdomsgradId)

        minimumSykdomsgrad.arbeidsgivere.forEach { arbeidsgiver ->
            asSQL(
                """
                INSERT INTO overstyring_minimum_sykdomsgrad_arbeidsgiver (berort_vedtaksperiode_id, arbeidsgiver_ref, overstyring_minimum_sykdomsgrad_ref)
                SELECT :beroertVedtaksperiodeId, ag.id, :overstyringMinimumSykdomsgradRef
                FROM arbeidsgiver ag
                WHERE ag.organisasjonsnummer = :organisasjonsnummer
                """.trimIndent(),
                "beroertVedtaksperiodeId" to arbeidsgiver.berørtVedtaksperiodeId,
                "overstyringMinimumSykdomsgradRef" to overstyringMinimumSykdomsgradId,
                "organisasjonsnummer" to arbeidsgiver.organisasjonsnummer,
            ).update()
        }
    }

    private fun insertOverstyrtMinimumSykdomsgradPerioder(
        perioder: List<MinimumSykdomsgradForDatabase.MinimumSykdomsgradPeriodeForDatabase>,
        vurdering: Boolean,
        overstyringMinimumSykdomsgradId: Long,
    ) {
        perioder.forEach { vurdertPeriode ->
            asSQL(
                """
                INSERT INTO overstyring_minimum_sykdomsgrad_periode (fom, tom, vurdering, overstyring_minimum_sykdomsgrad_ref)
                VALUES (:fom, :tom, :vurdering, :overstyringMinimumSykdomsgradRef)
                """.trimIndent(),
                "fom" to vurdertPeriode.fom,
                "tom" to vurdertPeriode.tom,
                "vurdering" to vurdering,
                "overstyringMinimumSykdomsgradRef" to overstyringMinimumSykdomsgradId,
            ).update()
        }
    }

    override fun persisterOverstyringArbeidsforhold(
        overstyrtArbeidsforhold: OverstyrtArbeidsforholdForDatabase,
        saksbehandlerOid: UUID,
    ) {
        val overstyringRef = insertIntoOverstyring(overstyrtArbeidsforhold, saksbehandlerOid)

        overstyrtArbeidsforhold.overstyrteArbeidsforhold.forEach { arbeidsforhold ->
            asSQL(
                """
                INSERT INTO overstyring_arbeidsforhold (forklaring, deaktivert, skjaeringstidspunkt, overstyring_ref, begrunnelse, arbeidsgiver_ref)
                SELECT :forklaring, :deaktivert, :skjaeringstidspunkt, :overstyringRef, :begrunnelse, ag.id
                FROM arbeidsgiver ag
                WHERE ag.organisasjonsnummer = :organisasjonsnummer
                """.trimIndent(),
                "forklaring" to arbeidsforhold.forklaring,
                "deaktivert" to arbeidsforhold.deaktivert,
                "skjaeringstidspunkt" to overstyrtArbeidsforhold.skjæringstidspunkt,
                "overstyringRef" to overstyringRef,
                "begrunnelse" to arbeidsforhold.begrunnelse,
                "organisasjonsnummer" to arbeidsforhold.organisasjonsnummer,
            ).update()
        }
    }

    private fun insertIntoOverstyring(
        request: OverstyringForDatabase,
        saksbehandlerOid: UUID,
    ): Long =
        asSQL(
            """
            INSERT INTO overstyring (hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt, vedtaksperiode_id)
            SELECT gen_random_uuid(), :eksternHendelseId, p.id, :saksbehandlerRef, :opprettet, :vedtaksperiodeId
            FROM person p
            WHERE p.fødselsnummer = :foedselsnummer
            """.trimIndent(),
            "eksternHendelseId" to request.eksternHendelseId,
            "foedselsnummer" to request.fødselsnummer,
            "saksbehandlerRef" to saksbehandlerOid,
            "opprettet" to request.opprettet,
            "vedtaksperiodeId" to request.vedtaksperiodeId,
        ).updateAndReturnGeneratedKey()
}
