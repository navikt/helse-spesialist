package no.nav.helse.spesialist.db.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrTilkommenInntekt
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringId
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.application.OverstyringRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.time.LocalDate
import java.util.UUID

class PgOverstyringRepository(
    session: Session,
) : QueryRunner by MedSession(session), OverstyringRepository {
    override fun lagre(
        overstyringer: List<Overstyring>,
        totrinnsvurderingId: TotrinnsvurderingId?,
    ) {
        overstyringer.forEach { overstyring ->
            if (overstyring.harFåttTildeltId()) {
                updateOverstyring(overstyring)
            } else {
                val id: Long =
                    when (overstyring) {
                        is OverstyrtTidslinje -> insertTidslinjeOverstyring(overstyring, totrinnsvurderingId)
                        is OverstyrtInntektOgRefusjon -> insertInntektOgRefusjonOverstyring(overstyring, totrinnsvurderingId)
                        is OverstyrtArbeidsforhold -> insertArbeidsforholdOverstyring(overstyring, totrinnsvurderingId)
                        is MinimumSykdomsgrad -> insertMinimumSykdomsgradOverstyring(overstyring, totrinnsvurderingId)
                        is SkjønnsfastsattSykepengegrunnlag -> insertSkjønnsfastsattSykepengegrunnlag(overstyring, totrinnsvurderingId)
                        is OverstyrTilkommenInntekt -> insertTilkommenInntekt(overstyring, totrinnsvurderingId)
                    }
                overstyring.tildelId(OverstyringId(id))
            }
        }
    }

    override fun finnAktive(
        fødselsnummer: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): List<Overstyring> =
        finnTidslinjeOverstyringer(fødselsnummer, totrinnsvurderingId) +
            finnInntektOgRefusjonOverstyringer(fødselsnummer, totrinnsvurderingId) +
            finnArbeidsforholdOverstyringer(fødselsnummer, totrinnsvurderingId) +
            finnMinimumSykdomsgradsOverstyringer(fødselsnummer, totrinnsvurderingId) +
            finnSkjønnsfastsattSykepengegrunnlag(fødselsnummer, totrinnsvurderingId) +
            finnTilkommenInntekt(fødselsnummer, totrinnsvurderingId)

    @Deprecated("Den andre skal tas i bruk på et eller annet tidspunkt")
    override fun finnAktive(fødselsnummer: String): List<Overstyring> =
        finnTidslinjeOverstyringer(fødselsnummer) +
            finnInntektOgRefusjonOverstyringer(fødselsnummer) +
            finnArbeidsforholdOverstyringer(fødselsnummer) +
            finnMinimumSykdomsgradsOverstyringer(fødselsnummer) +
            finnSkjønnsfastsattSykepengegrunnlag(fødselsnummer) +
            finnTilkommenInntekt(fødselsnummer)

    private fun insertOverstyring(
        overstyring: Overstyring,
        totrinnsvurderingId: TotrinnsvurderingId?,
    ): Long =
        asSQL(
            """
            INSERT INTO overstyring (hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt, vedtaksperiode_id, ferdigstilt, totrinnsvurdering_ref)
            SELECT gen_random_uuid(), :eksternHendelseId, p.id, :saksbehandlerRef, :opprettet, :vedtaksperiodeId, :ferdigstilt, :totrinnsvurderingId
            FROM person p
            WHERE p.fødselsnummer = :foedselsnummer
            """.trimIndent(),
            "eksternHendelseId" to overstyring.eksternHendelseId,
            "foedselsnummer" to overstyring.fødselsnummer,
            "saksbehandlerRef" to overstyring.saksbehandlerOid.value,
            "opprettet" to overstyring.opprettet,
            "vedtaksperiodeId" to overstyring.vedtaksperiodeId,
            "ferdigstilt" to overstyring.ferdigstilt,
            "totrinnsvurderingId" to totrinnsvurderingId?.value,
        ).updateAndReturnGeneratedKey()

    private fun insertTidslinjeOverstyring(
        overstyrtTidslinje: OverstyrtTidslinje,
        totrinnsvurderingId: TotrinnsvurderingId?,
    ): Long {
        val overstyringRef = insertOverstyring(overstyrtTidslinje, totrinnsvurderingId)

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

        return overstyringRef
    }

    private fun insertArbeidsforholdOverstyring(
        overstyrtArbeidsforhold: OverstyrtArbeidsforhold,
        totrinnsvurderingId: TotrinnsvurderingId?,
    ): Long {
        val overstyringRef = insertOverstyring(overstyrtArbeidsforhold, totrinnsvurderingId)

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

        return overstyringRef
    }

    private fun insertInntektOgRefusjonOverstyring(
        overstyrtInntektOgRefusjon: OverstyrtInntektOgRefusjon,
        totrinnsvurderingId: TotrinnsvurderingId?,
    ): Long {
        val overstyringRef = insertOverstyring(overstyrtInntektOgRefusjon, totrinnsvurderingId)

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

        return overstyringRef
    }

    private fun insertMinimumSykdomsgradOverstyring(
        minimumSykdomsgrad: MinimumSykdomsgrad,
        totrinnsvurderingId: TotrinnsvurderingId?,
    ): Long {
        val overstyringRef = insertOverstyring(minimumSykdomsgrad, totrinnsvurderingId)

        val overstyringMinimumSykdomsgradId =
            asSQL(
                """
                INSERT INTO overstyring_minimum_sykdomsgrad (overstyring_ref, fom, tom, vurdering, begrunnelse)
                VALUES (:overstyringRef, :fom, :tom, :vurdering, :begrunnelse)
                """.trimIndent(),
                "overstyringRef" to overstyringRef,
                "fom" to (
                    minimumSykdomsgrad.perioderVurdertOk.firstOrNull()?.fom
                        ?: minimumSykdomsgrad.perioderVurdertIkkeOk.first().fom
                ),
                "tom" to (
                    minimumSykdomsgrad.perioderVurdertOk.firstOrNull()?.tom
                        ?: minimumSykdomsgrad.perioderVurdertIkkeOk.first().tom
                ),
                "vurdering" to (minimumSykdomsgrad.perioderVurdertOk.isNotEmpty()),
                "begrunnelse" to minimumSykdomsgrad.begrunnelse,
            ).updateAndReturnGeneratedKey()

        insertOverstyrtMinimumSykdomsgradPerioder(
            minimumSykdomsgrad.perioderVurdertOk,
            true,
            overstyringMinimumSykdomsgradId,
        )
        insertOverstyrtMinimumSykdomsgradPerioder(
            minimumSykdomsgrad.perioderVurdertIkkeOk,
            false,
            overstyringMinimumSykdomsgradId,
        )

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

        return overstyringRef
    }

    private fun insertSkjønnsfastsattSykepengegrunnlag(
        skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlag,
        totrinnsvurderingId: TotrinnsvurderingId?,
    ): Long {
        // Den felles informasjonen ligger på alle arbeidsgiverne. Burde kanskje skilles ut i eget objekt
        val enArbeidsgiver = skjønnsfastsattSykepengegrunnlag.arbeidsgivere.first()

        val (begrunnelseFritekstId, begrunnelseMalId, begrunnelseKonklusjonId) =
            mapOf(
                "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_FRITEKST" to enArbeidsgiver.begrunnelseFritekst,
                "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_MAL" to enArbeidsgiver.begrunnelseMal,
                "SKJØNNSFASTSATT_SYKEPENGEGRUNNLAG_KONKLUSJON" to enArbeidsgiver.begrunnelseKonklusjon,
            ).map { (type, tekst) ->
                checkNotNull(tekst)
                insertBegrunnelse(type, tekst, skjønnsfastsattSykepengegrunnlag.saksbehandlerOid)
            }

        val overstyringRef = insertOverstyring(skjønnsfastsattSykepengegrunnlag, totrinnsvurderingId)

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

        return overstyringRef
    }

    private fun insertTilkommenInntekt(
        tilkommenInntekt: OverstyrTilkommenInntekt,
        totrinnsvurderingId: TotrinnsvurderingId?,
    ): Long {
        val overstyringRef = insertOverstyring(tilkommenInntekt, totrinnsvurderingId)

        val dbTilkommenInntekt =
            DbTilkommenInntekt(
                nyeEllerEndredeInntekter =
                    tilkommenInntekt.nyEllerEndredeInntekter.map { nyEllerEndret ->
                        DbTilkommenInntekt.DbNyEllerEndretInntekt(
                            organisasjonsnummer = nyEllerEndret.organisasjonsnummer,
                            perioder =
                                nyEllerEndret.perioder.map {
                                    DbTilkommenInntekt.DbNyEllerEndretInntekt.DbPeriodeMedBeløp(it.fom, it.tom, it.periodeBeløp)
                                },
                        )
                    },
                fjernedeInntekter =
                    tilkommenInntekt.fjernedeInntekter.map { fjernet ->
                        DbTilkommenInntekt.DbFjernetInntekt(
                            organisasjonsnummer = fjernet.organisasjonsnummer,
                            perioder =
                                fjernet.perioder.map {
                                    DbTilkommenInntekt.DbFjernetInntekt.DbPeriodeUtenBeløp(it.fom, it.tom)
                                },
                        )
                    },
            )

        asSQL(
            """
                INSERT INTO overstyring_tilkommen_inntekt (overstyring_ref, json)
                VALUES (:overstyring_ref, :json::jsonb)
            """,
            "overstyring_ref" to overstyringRef,
            "json" to objectMapper.writeValueAsString(dbTilkommenInntekt),
        ).update()

        return overstyringRef
    }

    private fun insertOverstyrtMinimumSykdomsgradPerioder(
        perioder: List<MinimumSykdomsgradPeriode>,
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

    private fun insertBegrunnelse(
        type: String,
        tekst: String,
        saksbehandlerOid: SaksbehandlerOid,
    ): Long =
        asSQL(
            """
            INSERT INTO begrunnelse (type, tekst, saksbehandler_ref)
            VALUES (:type, :tekst, :saksbehandlerRef)
            """.trimIndent(),
            "type" to type,
            "tekst" to tekst,
            "saksbehandlerRef" to saksbehandlerOid.value,
        ).updateAndReturnGeneratedKey()

    private fun updateOverstyring(overstyring: Overstyring) {
        asSQL(
            "UPDATE overstyring SET ferdigstilt = :ferdigstilt WHERE id = :overstyringId".trimIndent(),
            "overstyringId" to overstyring.id().value,
            "ferdigstilt" to overstyring.ferdigstilt,
        ).update()
    }

    private fun finnTidslinjeOverstyringer(
        fødselsnummer: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): List<OverstyrtTidslinje> =
        asSQL(
            """
            SELECT o.id,
                   ot.id AS overstyring_tidslinje_id,
                   o.ekstern_hendelse_id,
                   p.fødselsnummer,
                   p.aktør_id,
                   a.organisasjonsnummer,
                   o.vedtaksperiode_id,
                   ot.begrunnelse,
                   o.tidspunkt,
                   o.ferdigstilt,
                   o.saksbehandler_ref
            FROM overstyring o
                INNER JOIN overstyring_tidslinje ot ON ot.overstyring_ref = o.id
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a ON a.id = ot.arbeidsgiver_ref
            WHERE p.fødselsnummer = :fodselsnummer and o.totrinnsvurdering_ref = :totrinnsvurderingId and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
            "totrinnsvurderingId" to totrinnsvurderingId.value,
        ).list { it.toOverstyrtTidslinje() }

    @Deprecated("Den andre skal tas i bruk på et eller annet tidspunkt")
    private fun finnTidslinjeOverstyringer(fødselsnummer: String): List<OverstyrtTidslinje> =
        asSQL(
            """
            SELECT o.id,
                   ot.id AS overstyring_tidslinje_id,
                   o.ekstern_hendelse_id,
                   p.fødselsnummer,
                   p.aktør_id,
                   a.organisasjonsnummer,
                   o.vedtaksperiode_id,
                   ot.begrunnelse,
                   o.tidspunkt,
                   o.ferdigstilt,
                   o.saksbehandler_ref
            FROM overstyring o
                INNER JOIN overstyring_tidslinje ot ON ot.overstyring_ref = o.id
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a ON a.id = ot.arbeidsgiver_ref
            WHERE p.fødselsnummer = :fodselsnummer and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
        ).list { it.toOverstyrtTidslinje() }

    private fun finnInntektOgRefusjonOverstyringer(
        fødselsnummer: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): List<OverstyrtInntektOgRefusjon> =
        asSQL(
            """
            SELECT DISTINCT ON (o.id)
                o.id,
                o.ekstern_hendelse_id,
                p.fødselsnummer,
                p.aktør_id,
                o.tidspunkt,
                o.vedtaksperiode_id,
                o.saksbehandler_ref,
                o.ferdigstilt,
                oi.skjaeringstidspunkt
            FROM overstyring o
                INNER JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
            WHERE p.fødselsnummer = :fodselsnummer and o.totrinnsvurdering_ref = :totrinnsvurderingId and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
            "totrinnsvurderingId" to totrinnsvurderingId.value,
        ).list { it.toOverstyrtInntektOgRefusjon() }

    @Deprecated("Den andre skal tas i bruk på et eller annet tidspunkt")
    private fun finnInntektOgRefusjonOverstyringer(fødselsnummer: String): List<OverstyrtInntektOgRefusjon> =
        asSQL(
            """
            SELECT DISTINCT ON (o.id)
                o.id,
                o.ekstern_hendelse_id,
                p.fødselsnummer,
                p.aktør_id,
                o.tidspunkt,
                o.vedtaksperiode_id,
                o.saksbehandler_ref,
                o.ferdigstilt,
                oi.skjaeringstidspunkt
            FROM overstyring o
                INNER JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
            WHERE p.fødselsnummer = :fodselsnummer and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
        ).list { it.toOverstyrtInntektOgRefusjon() }

    private fun finnArbeidsforholdOverstyringer(
        fødselsnummer: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): List<OverstyrtArbeidsforhold> =
        asSQL(
            """
            SELECT DISTINCT ON (o.id)
                o.id,
                o.ekstern_hendelse_id,
                p.fødselsnummer,
                o.tidspunkt,
                p.aktør_id,
                o.ferdigstilt,
                o.vedtaksperiode_id,
                o.saksbehandler_ref,
                o.ferdigstilt,
                oa.skjaeringstidspunkt
            FROM overstyring o
                INNER JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
            WHERE p.fødselsnummer = :fodselsnummer and o.totrinnsvurdering_ref = :totrinnsvurderingId and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
            "totrinnsvurderingId" to totrinnsvurderingId.value,
        ).list { it.toOverstyrtArbeidsforhold() }

    @Deprecated("Den andre skal tas i bruk på et eller annet tidspunkt")
    private fun finnArbeidsforholdOverstyringer(fødselsnummer: String): List<OverstyrtArbeidsforhold> =
        asSQL(
            """
            SELECT DISTINCT ON (o.id)
                o.id,
                o.ekstern_hendelse_id,
                p.fødselsnummer,
                o.tidspunkt,
                p.aktør_id,
                o.ferdigstilt,
                o.vedtaksperiode_id,
                o.saksbehandler_ref,
                o.ferdigstilt,
                oa.skjaeringstidspunkt
            FROM overstyring o
                INNER JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
            WHERE p.fødselsnummer = :fodselsnummer and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
        ).list { it.toOverstyrtArbeidsforhold() }

    private fun finnMinimumSykdomsgradsOverstyringer(
        fødselsnummer: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): List<MinimumSykdomsgrad> =
        asSQL(
            """
            SELECT
                o.id,
                oms.id AS overstyring_minimum_sykdomsgrad_id,
                ekstern_hendelse_id,
                aktør_id,
                fødselsnummer,
                tidspunkt,
                vedtaksperiode_id,
                begrunnelse,
                o.saksbehandler_ref,
                o.ferdigstilt
            FROM overstyring o
                JOIN person p ON o.person_ref = p.id
                JOIN overstyring_minimum_sykdomsgrad oms ON oms.overstyring_ref = o.id
             WHERE p.fødselsnummer = :fodselsnummer and o.totrinnsvurdering_ref = :totrinnsvurderingId and o.ferdigstilt = false
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
            "totrinnsvurderingId" to totrinnsvurderingId.value,
        ).list { it.toMinimumSykdomsgrad() }

    @Deprecated("Den andre skal tas i bruk på et eller annet tidspunkt")
    private fun finnMinimumSykdomsgradsOverstyringer(fødselsnummer: String): List<MinimumSykdomsgrad> =
        asSQL(
            """
            SELECT
                o.id,
                oms.id AS overstyring_minimum_sykdomsgrad_id,
                ekstern_hendelse_id,
                aktør_id,
                fødselsnummer,
                tidspunkt,
                vedtaksperiode_id,
                begrunnelse,
                o.saksbehandler_ref,
                o.ferdigstilt
            FROM overstyring o
                JOIN person p ON o.person_ref = p.id
                JOIN overstyring_minimum_sykdomsgrad oms ON oms.overstyring_ref = o.id
             WHERE p.fødselsnummer = :fodselsnummer and o.ferdigstilt = false
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).list { it.toMinimumSykdomsgrad() }

    private fun finnSkjønnsfastsattSykepengegrunnlag(
        fødselsnummer: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): List<SkjønnsfastsattSykepengegrunnlag> =
        asSQL(
            """
            SELECT o.id,
                   ss.id    as overstyring_skjonn_id,
                   ss.subsumsjon,
                   o.ekstern_hendelse_id,
                   p.fødselsnummer,
                   p.aktør_id,
                   o.tidspunkt,
                   ss.skjaeringstidspunkt,
                   o.vedtaksperiode_id,
                   o.saksbehandler_ref,
                   ss.arsak,
                   ss.type,
                   b2.tekst as mal,
                   b1.tekst as fritekst,
                   b3.tekst as konklusjon,
                   ss.initierende_vedtaksperiode_id,
                   o.ferdigstilt
            FROM overstyring o
                     INNER JOIN skjonnsfastsetting_sykepengegrunnlag ss ON o.id = ss.overstyring_ref
                     INNER JOIN person p ON p.id = o.person_ref
                     INNER JOIN begrunnelse b1 ON ss.begrunnelse_fritekst_ref = b1.id
                     INNER JOIN begrunnelse b2 ON ss.begrunnelse_mal_ref = b2.id
                     INNER JOIN begrunnelse b3 ON ss.begrunnelse_konklusjon_ref = b3.id
            WHERE p.fødselsnummer = :fodselsnummer and o.totrinnsvurdering_ref = :totrinnsvurderingId and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
            "totrinnsvurderingId" to totrinnsvurderingId.value,
        ).list { it.toSkjønnsfastsattSykepengegrunnlag() }

    @Deprecated("Den andre skal tas i bruk på et eller annet tidspunkt")
    private fun finnSkjønnsfastsattSykepengegrunnlag(fødselsnummer: String): List<SkjønnsfastsattSykepengegrunnlag> =
        asSQL(
            """
            SELECT o.id,
                   ss.id    as overstyring_skjonn_id,
                   ss.subsumsjon,
                   o.ekstern_hendelse_id,
                   p.fødselsnummer,
                   p.aktør_id,
                   o.tidspunkt,
                   ss.skjaeringstidspunkt,
                   o.vedtaksperiode_id,
                   o.saksbehandler_ref,
                   ss.arsak,
                   ss.type,
                   b2.tekst as mal,
                   b1.tekst as fritekst,
                   b3.tekst as konklusjon,
                   ss.initierende_vedtaksperiode_id,
                   o.ferdigstilt
            FROM overstyring o
                     INNER JOIN skjonnsfastsetting_sykepengegrunnlag ss ON o.id = ss.overstyring_ref
                     INNER JOIN person p ON p.id = o.person_ref
                     INNER JOIN begrunnelse b1 ON ss.begrunnelse_fritekst_ref = b1.id
                     INNER JOIN begrunnelse b2 ON ss.begrunnelse_mal_ref = b2.id
                     INNER JOIN begrunnelse b3 ON ss.begrunnelse_konklusjon_ref = b3.id
            WHERE p.fødselsnummer = :fodselsnummer and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
        ).list { it.toSkjønnsfastsattSykepengegrunnlag() }

    private fun finnTilkommenInntekt(
        fødselsnummer: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): List<OverstyrTilkommenInntekt> =
        asSQL(
            """
            SELECT o.id,
                   o.ekstern_hendelse_id,
                   p.fødselsnummer,
                   p.aktør_id,
                   o.tidspunkt,
                   o.vedtaksperiode_id,
                   o.saksbehandler_ref,
                   o.ferdigstilt,
                   oti.json
            FROM overstyring o
                     INNER JOIN overstyring_tilkommen_inntekt oti ON o.id = oti.overstyring_ref
                     INNER JOIN person p ON p.id = o.person_ref
            WHERE p.fødselsnummer = :fodselsnummer and o.totrinnsvurdering_ref = :totrinnsvurderingId and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
            "totrinnsvurderingId" to totrinnsvurderingId.value,
        ).list { it.toTilkommenInntekt() }

    @Deprecated("Den andre skal tas i bruk på et eller annet tidspunkt")
    private fun finnTilkommenInntekt(fødselsnummer: String): List<OverstyrTilkommenInntekt> =
        asSQL(
            """
            SELECT o.id,
                   o.ekstern_hendelse_id,
                   p.fødselsnummer,
                   p.aktør_id,
                   o.tidspunkt,
                   o.vedtaksperiode_id,
                   o.saksbehandler_ref,
                   o.ferdigstilt,
                   oti.json
            FROM overstyring o
                     INNER JOIN overstyring_tilkommen_inntekt oti ON o.id = oti.overstyring_ref
                     INNER JOIN person p ON p.id = o.person_ref
            WHERE p.fødselsnummer = :fodselsnummer and o.ferdigstilt = false
            """,
            "fodselsnummer" to fødselsnummer,
        ).list { it.toTilkommenInntekt() }

    private fun finnSkjønnsfastsattArbeidsgiver(overstyringRow: Row): List<SkjønnsfastsattArbeidsgiver> =
        asSQL(
            """
                SELECT arlig, fra_arlig, organisasjonsnummer FROM skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver ssa
                    JOIN arbeidsgiver a ON a.id = ssa.arbeidsgiver_ref
                WHERE skjonnsfastsetting_sykepengegrunnlag_ref = :id
                """,
            "id" to overstyringRow.long("overstyring_skjonn_id"),
        ).list { it.toSkjønnsfastsattArbeidsgiver(overstyringRow) }

    private fun finnMinimumSykdomsgradArbeidsgiver(id: Long): List<MinimumSykdomsgradArbeidsgiver> =
        asSQL(
            """
            SELECT berort_vedtaksperiode_id, organisasjonsnummer
            FROM overstyring_minimum_sykdomsgrad_arbeidsgiver omsa
                 JOIN arbeidsgiver a ON omsa.arbeidsgiver_ref = a.id
            WHERE omsa.id = :id
            """,
            "id" to id,
        ).list { it.toMinimumSykdomsgradArbeidsgiver() }

    private fun finnOverstyrtTidslinjeDager(id: Long): List<OverstyrtTidslinjedag> =
        asSQL(
            """
            SELECT dato, dagtype, fra_dagtype, grad, fra_grad
            FROM overstyring_dag
            WHERE overstyring_tidslinje_ref = :id
            """,
            "id" to id,
        ).list { it.toOverstyrtTidslinjeDag() }

    private fun finnOverstyrtArbeidsgiver(id: Long): List<OverstyrtArbeidsgiver> =
        asSQL(
            """
            SELECT organisasjonsnummer,
                   manedlig_inntekt,
                   fra_manedlig_inntekt,
                   begrunnelse,
                   forklaring,
                   fom,
                   tom,
                   refusjonsopplysninger,
                   fra_refusjonsopplysninger,
                   subsumsjon
            FROM overstyring_inntekt oi
                     JOIN arbeidsgiver a ON oi.arbeidsgiver_ref = a.id
            WHERE overstyring_ref = :id
            """.trimIndent(),
            "id" to id,
        ).list { it.toOverstyrtArbeidsgiver() }

    private fun finnArbeidsforhold(id: Long): List<Arbeidsforhold> =
        asSQL(
            """
            SELECT organisasjonsnummer, deaktivert, begrunnelse, forklaring
            FROM overstyring_arbeidsforhold oa
                JOIN arbeidsgiver a ON oa.arbeidsgiver_ref = a.id
            WHERE overstyring_ref = :id
            """,
            "id" to id,
        ).list { it.toArbeidsforhold() }

    private fun Row.toOverstyrtTidslinje(): OverstyrtTidslinje {
        val id = OverstyringId(long("id"))
        return OverstyrtTidslinje.fraLagring(
            id = id,
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            organisasjonsnummer = string("organisasjonsnummer"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            begrunnelse = string("begrunnelse"),
            opprettet = localDateTime("tidspunkt"),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            ferdigstilt = boolean("ferdigstilt"),
            dager = finnOverstyrtTidslinjeDager(long("overstyring_tidslinje_id")),
            kobledeVedtaksperioder = finnKobledeVedtaksperioderForOverstyring(id),
        )
    }

    private fun Row.toOverstyrtInntektOgRefusjon(): OverstyrtInntektOgRefusjon {
        val id = OverstyringId(long("id"))
        return OverstyrtInntektOgRefusjon.fraLagring(
            id = id,
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            opprettet = localDateTime("tidspunkt"),
            skjæringstidspunkt = localDate("skjaeringstidspunkt"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            ferdigstilt = boolean("ferdigstilt"),
            arbeidsgivere = finnOverstyrtArbeidsgiver(long("id")),
            kobledeVedtaksperioder = finnKobledeVedtaksperioderForOverstyring(id),
        )
    }

    private fun Row.toOverstyrtArbeidsforhold(): OverstyrtArbeidsforhold {
        val id = OverstyringId(long("id"))
        return OverstyrtArbeidsforhold.fraLagring(
            id = id,
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            opprettet = localDateTime("tidspunkt"),
            skjæringstidspunkt = localDate("skjaeringstidspunkt"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            ferdigstilt = boolean("ferdigstilt"),
            overstyrteArbeidsforhold = finnArbeidsforhold(long("id")),
            kobledeVedtaksperioder = finnKobledeVedtaksperioderForOverstyring(id),
        )
    }

    private fun Row.toMinimumSykdomsgrad(): MinimumSykdomsgrad {
        val minimumSykdomsgradId = long("overstyring_minimum_sykdomsgrad_id")
        val (ok, ikkeOk) =
            asSQL(
                "SELECT vurdering, fom, tom FROM overstyring_minimum_sykdomsgrad_periode WHERE overstyring_minimum_sykdomsgrad_ref = :id",
                "id" to minimumSykdomsgradId,
            ).list { it.toMinimumSykdomsgradPeriode() }
                .partition { (vurdering, _) -> vurdering }

        val perioderSomErOk = ok.map { it.second }
        val perioderSomIkkeErOk = ikkeOk.map { it.second }

        val id = OverstyringId(long("id"))

        return MinimumSykdomsgrad.fraLagring(
            id = id,
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            aktørId = string("aktør_id"),
            fødselsnummer = string("fødselsnummer"),
            perioderVurdertOk = perioderSomErOk,
            perioderVurdertIkkeOk = perioderSomIkkeErOk,
            begrunnelse = string("begrunnelse"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            arbeidsgivere = finnMinimumSykdomsgradArbeidsgiver(minimumSykdomsgradId),
            opprettet = localDateTime("tidspunkt"),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            ferdigstilt = boolean("ferdigstilt"),
            kobledeVedtaksperioder = finnKobledeVedtaksperioderForOverstyring(id),
        )
    }

    private fun finnKobledeVedtaksperioderForOverstyring(overstyringRef: OverstyringId): List<UUID> =
        asSQL(
            "SELECT vedtaksperiode_id FROM overstyringer_for_vedtaksperioder WHERE overstyring_ref = :overstyringRef",
            "overstyringRef" to overstyringRef.value,
        ).list { row -> row.uuid("vedtaksperiode_id") }

    private fun Row.toSkjønnsfastsattSykepengegrunnlag(): SkjønnsfastsattSykepengegrunnlag {
        val id = OverstyringId(long("id"))
        return SkjønnsfastsattSykepengegrunnlag.fraLagring(
            id = id,
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            opprettet = localDateTime("tidspunkt"),
            skjæringstidspunkt = localDate("skjaeringstidspunkt"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            ferdigstilt = boolean("ferdigstilt"),
            arbeidsgivere = finnSkjønnsfastsattArbeidsgiver(this),
            kobledeVedtaksperioder = finnKobledeVedtaksperioderForOverstyring(id),
        )
    }

    private fun Row.toTilkommenInntekt(): OverstyrTilkommenInntekt {
        val dbTilkommenInntekt = objectMapper.readValue<DbTilkommenInntekt>(this.string("json"))
        val id = OverstyringId(long("id"))
        return OverstyrTilkommenInntekt.fraLagring(
            id = id,
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            opprettet = localDateTime("tidspunkt"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            ferdigstilt = boolean("ferdigstilt"),
            nyeEllerEndredeInntekter =
                dbTilkommenInntekt.nyeEllerEndredeInntekter.map { nyEllerEndret ->
                    OverstyrTilkommenInntekt.NyEllerEndretInntekt(
                        organisasjonsnummer = nyEllerEndret.organisasjonsnummer,
                        perioder =
                            nyEllerEndret.perioder.map {
                                OverstyrTilkommenInntekt.NyEllerEndretInntekt.PeriodeMedBeløp(
                                    fom = it.fom,
                                    tom = it.tom,
                                    periodeBeløp = it.periodeBeløp,
                                )
                            },
                    )
                },
            fjernedeInntekter =
                dbTilkommenInntekt.fjernedeInntekter.map { fjernet ->
                    OverstyrTilkommenInntekt.FjernetInntekt(
                        organisasjonsnummer = fjernet.organisasjonsnummer,
                        perioder =
                            fjernet.perioder.map {
                                OverstyrTilkommenInntekt.FjernetInntekt.PeriodeUtenBeløp(fom = it.fom, tom = it.tom)
                            },
                    )
                },
            kobledeVedtaksperioder = finnKobledeVedtaksperioderForOverstyring(id),
        )
    }

    private fun Row.toSkjønnsfastsattArbeidsgiver(overstyringRow: Row): SkjønnsfastsattArbeidsgiver =
        SkjønnsfastsattArbeidsgiver(
            organisasjonsnummer = string("organisasjonsnummer"),
            årlig = double("arlig"),
            fraÅrlig = double("fra_arlig"),
            årsak = overstyringRow.string("arsak"),
            type = enumValueOf(overstyringRow.string("type")),
            begrunnelseMal = overstyringRow.string("mal"),
            begrunnelseFritekst = overstyringRow.string("fritekst"),
            begrunnelseKonklusjon = overstyringRow.string("konklusjon"),
            lovhjemmel = null,
            initierendeVedtaksperiodeId = overstyringRow.stringOrNull("initierende_vedtaksperiode_id"),
        )

    private fun Row.toMinimumSykdomsgradPeriode(): Pair<Boolean, MinimumSykdomsgradPeriode> =
        boolean("vurdering") to
            MinimumSykdomsgradPeriode(
                localDate("fom"),
                localDate("tom"),
            )

    private fun Row.toMinimumSykdomsgradArbeidsgiver(): MinimumSykdomsgradArbeidsgiver =
        MinimumSykdomsgradArbeidsgiver(
            organisasjonsnummer = string("organisasjonsnummer"),
            berørtVedtaksperiodeId = uuid("berort_vedtaksperiode_id"),
        )

    private fun Row.toOverstyrtArbeidsgiver(): OverstyrtArbeidsgiver =
        OverstyrtArbeidsgiver(
            organisasjonsnummer = string("organisasjonsnummer"),
            månedligInntekt = double("manedlig_inntekt"),
            fraMånedligInntekt = double("fra_manedlig_inntekt"),
            begrunnelse = string("begrunnelse"),
            forklaring = string("forklaring"),
            fom = localDateOrNull("fom"),
            tom = localDateOrNull("tom"),
            refusjonsopplysninger =
                stringOrNull("refusjonsopplysninger")
                    ?.let { objectMapper.readValue(it) },
            fraRefusjonsopplysninger =
                stringOrNull("fra_refusjonsopplysninger")
                    ?.let { objectMapper.readValue(it) },
            lovhjemmel = null,
        )

    private fun Row.toArbeidsforhold(): Arbeidsforhold =
        Arbeidsforhold(
            organisasjonsnummer = string("organisasjonsnummer"),
            deaktivert = boolean("deaktivert"),
            begrunnelse = string("begrunnelse"),
            forklaring = string("forklaring"),
            lovhjemmel = null,
        )

    private fun Row.toOverstyrtTidslinjeDag(): OverstyrtTidslinjedag =
        OverstyrtTidslinjedag(
            dato = localDate("dato"),
            type = string("dagtype"),
            fraType = string("fra_dagtype"),
            grad = intOrNull("grad"),
            fraGrad = intOrNull("fra_grad"),
            lovhjemmel = null,
        )

    private class DbTilkommenInntekt(
        val nyeEllerEndredeInntekter: List<DbNyEllerEndretInntekt>,
        val fjernedeInntekter: List<DbFjernetInntekt>,
    ) {
        data class DbNyEllerEndretInntekt(val organisasjonsnummer: String, val perioder: List<DbPeriodeMedBeløp>) {
            data class DbPeriodeMedBeløp(val fom: LocalDate, val tom: LocalDate, val periodeBeløp: Double)
        }

        data class DbFjernetInntekt(val organisasjonsnummer: String, val perioder: List<DbPeriodeUtenBeløp>) {
            data class DbPeriodeUtenBeløp(val fom: LocalDate, val tom: LocalDate)
        }
    }
}
