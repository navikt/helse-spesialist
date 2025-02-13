package no.nav.helse.spesialist.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringId
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.application.OverstyringRepository
import java.util.UUID

class PgOverstyringRepository(
    private val session: Session,
) : QueryRunner by MedSession(session), OverstyringRepository {
    override fun lagre(overstyringer: List<Overstyring>) {
        overstyringer.forEach { overstyring ->
            when (overstyring) {
                is OverstyrtTidslinje -> lagre(overstyring)
                is OverstyrtInntektOgRefusjon -> lagre(overstyring)
                is OverstyrtArbeidsforhold -> lagre(overstyring)
                is MinimumSykdomsgrad -> lagre(overstyring)
                is SkjønnsfastsattSykepengegrunnlag -> lagre(overstyring)
            }
        }
    }

    override fun finn(fødselsnummer: String): List<Overstyring> =
        finnTidslinjeOverstyringer(fødselsnummer) +
            finnInntektOgRefusjonOverstyringer(fødselsnummer) +
            finnArbeidsforholdOverstyringer(fødselsnummer) +
            finnMinimumSykdomsgradsOverstyringer(fødselsnummer) +
            finnSkjønnsfastsattSykepengegrunnlag(fødselsnummer)

    private fun lagre(tidslinjeOverstyring: OverstyrtTidslinje) {
        if (!tidslinjeOverstyring.harFåttTildeltId()) {
            insertTidslinjeOverstyring(tidslinjeOverstyring)
                .let(::OverstyringId)
                .let(tidslinjeOverstyring::tildelId)
        } else {
            updateOverstyring(tidslinjeOverstyring)
        }
    }

    private fun lagre(overstyrtArbeidsforhold: OverstyrtArbeidsforhold) {
        if (!overstyrtArbeidsforhold.harFåttTildeltId()) {
            insertArbeidsforholdOverstyring(overstyrtArbeidsforhold)
                .let(::OverstyringId)
                .let(overstyrtArbeidsforhold::tildelId)
        } else {
            updateOverstyring(overstyrtArbeidsforhold)
        }
    }

    private fun lagre(overstyrtInntektOgRefusjon: OverstyrtInntektOgRefusjon) {
        if (!overstyrtInntektOgRefusjon.harFåttTildeltId()) {
            insertInntektOgRefusjonOverstyring(overstyrtInntektOgRefusjon)
                .let(::OverstyringId)
                .let(overstyrtInntektOgRefusjon::tildelId)
        } else {
            updateOverstyring(overstyrtInntektOgRefusjon)
        }
    }

    private fun lagre(minimumSykdomsgrad: MinimumSykdomsgrad) {
        if (!minimumSykdomsgrad.harFåttTildeltId()) {
            insertMinimumSykdomsgradOverstyring(minimumSykdomsgrad)
                .let(::OverstyringId)
                .let(minimumSykdomsgrad::tildelId)
        } else {
            updateOverstyring(minimumSykdomsgrad)
        }
    }

    private fun lagre(skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlag) {
        if (!skjønnsfastsattSykepengegrunnlag.harFåttTildeltId()) {
            insertSkjønnsfastsattSykepengegrunnlag(skjønnsfastsattSykepengegrunnlag)
                .let(::OverstyringId)
                .let(skjønnsfastsattSykepengegrunnlag::tildelId)
        } else {
            updateOverstyring(skjønnsfastsattSykepengegrunnlag)
        }
    }

    private fun insertOverstyring(overstyring: Overstyring): Long =
        asSQL(
            """
            INSERT INTO overstyring (hendelse_ref, ekstern_hendelse_id, person_ref, saksbehandler_ref, tidspunkt, vedtaksperiode_id, ferdigstilt)
            SELECT gen_random_uuid(), :eksternHendelseId, p.id, :saksbehandlerRef, :opprettet, :vedtaksperiodeId, :ferdigstilt
            FROM person p
            WHERE p.fødselsnummer = :foedselsnummer
            """.trimIndent(),
            "eksternHendelseId" to overstyring.eksternHendelseId,
            "foedselsnummer" to overstyring.fødselsnummer,
            "saksbehandlerRef" to overstyring.saksbehandlerOid,
            "opprettet" to overstyring.opprettet,
            "vedtaksperiodeId" to overstyring.vedtaksperiodeId,
            "ferdigstilt" to overstyring.ferdigstilt,
        ).updateAndReturnGeneratedKey()

    private fun insertTidslinjeOverstyring(overstyrtTidslinje: OverstyrtTidslinje): Long {
        val overstyringRef = insertOverstyring(overstyrtTidslinje)

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

    private fun insertArbeidsforholdOverstyring(overstyrtArbeidsforhold: OverstyrtArbeidsforhold): Long {
        val overstyringRef = insertOverstyring(overstyrtArbeidsforhold)

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

    private fun insertInntektOgRefusjonOverstyring(overstyrtInntektOgRefusjon: OverstyrtInntektOgRefusjon): Long {
        val overstyringRef = insertOverstyring(overstyrtInntektOgRefusjon)

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

    private fun insertMinimumSykdomsgradOverstyring(minimumSykdomsgrad: MinimumSykdomsgrad): Long {
        val overstyringRef = insertOverstyring(minimumSykdomsgrad)

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

    private fun insertSkjønnsfastsattSykepengegrunnlag(skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlag): Long {
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

        val overstyringRef = insertOverstyring(skjønnsfastsattSykepengegrunnlag)

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
        saksbehandlerOid: UUID,
    ): Long =
        asSQL(
            """
            INSERT INTO begrunnelse (type, tekst, saksbehandler_ref)
            VALUES (:type, :tekst, :saksbehandlerRef)
            """.trimIndent(),
            "type" to type,
            "tekst" to tekst,
            "saksbehandlerRef" to saksbehandlerOid,
        ).updateAndReturnGeneratedKey()

    private fun updateOverstyring(overstyring: Overstyring) {
        asSQL(
            "UPDATE overstyring SET ferdigstilt = :ferdigstilt WHERE id = :overstyringId".trimIndent(),
            "overstyringId" to overstyring.id(),
            "ferdigstilt" to overstyring.ferdigstilt,
        ).update()
    }

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

    private fun Row.toOverstyrtTidslinje(): OverstyrtTidslinje =
        OverstyrtTidslinje.fraLagring(
            id = OverstyringId(long("id")),
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            organisasjonsnummer = string("organisasjonsnummer"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            begrunnelse = string("begrunnelse"),
            opprettet = localDateTime("tidspunkt"),
            saksbehandlerOid = uuid("saksbehandler_ref"),
            ferdigstilt = boolean("ferdigstilt"),
            dager = finnOverstyrtTidslinjeDager(long("overstyring_tidslinje_id")),
        )

    private fun Row.toOverstyrtInntektOgRefusjon(): OverstyrtInntektOgRefusjon =
        OverstyrtInntektOgRefusjon.fraLagring(
            id = OverstyringId(long("id")),
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            opprettet = localDateTime("tidspunkt"),
            skjæringstidspunkt = localDate("skjaeringstidspunkt"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            saksbehandlerOid = uuid("saksbehandler_ref"),
            ferdigstilt = boolean("ferdigstilt"),
            arbeidsgivere = finnOverstyrtArbeidsgiver(long("id")),
        )

    private fun Row.toOverstyrtArbeidsforhold(): OverstyrtArbeidsforhold =
        OverstyrtArbeidsforhold.fraLagring(
            id = OverstyringId(long("id")),
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            opprettet = localDateTime("tidspunkt"),
            skjæringstidspunkt = localDate("skjaeringstidspunkt"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            saksbehandlerOid = uuid("saksbehandler_ref"),
            ferdigstilt = boolean("ferdigstilt"),
            overstyrteArbeidsforhold = finnArbeidsforhold(long("id")),
        )

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

        return MinimumSykdomsgrad.fraLagring(
            id = OverstyringId(long("id")),
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            aktørId = string("aktør_id"),
            fødselsnummer = string("fødselsnummer"),
            perioderVurdertOk = perioderSomErOk,
            perioderVurdertIkkeOk = perioderSomIkkeErOk,
            begrunnelse = string("begrunnelse"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            arbeidsgivere = finnMinimumSykdomsgradArbeidsgiver(minimumSykdomsgradId),
            opprettet = localDateTime("tidspunkt"),
            saksbehandlerOid = uuid("saksbehandler_ref"),
            ferdigstilt = boolean("ferdigstilt"),
        )
    }

    private fun Row.toSkjønnsfastsattSykepengegrunnlag(): SkjønnsfastsattSykepengegrunnlag =
        SkjønnsfastsattSykepengegrunnlag.fraLagring(
            id = OverstyringId(long("id")),
            eksternHendelseId = uuid("ekstern_hendelse_id"),
            fødselsnummer = string("fødselsnummer"),
            aktørId = string("aktør_id"),
            opprettet = localDateTime("tidspunkt"),
            skjæringstidspunkt = localDate("skjaeringstidspunkt"),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            saksbehandlerOid = uuid("saksbehandler_ref"),
            ferdigstilt = boolean("ferdigstilt"),
            arbeidsgivere = finnSkjønnsfastsattArbeidsgiver(this),
        )

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
            lovhjemmel = overstyringRow.stringOrNull("subsumsjon")?.let { objectMapper.readValue(it) },
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
            lovhjemmel =
                stringOrNull("subsumsjon")
                    ?.let { objectMapper.readValue(it) },
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
}
