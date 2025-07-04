package no.nav.helse.spesialist.db.dao.api

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringArbeidsforholdDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringDto
import no.nav.helse.spesialist.api.overstyring.OverstyringInntektDto
import no.nav.helse.spesialist.api.overstyring.OverstyringMinimumSykdomsgradDto
import no.nav.helse.spesialist.api.overstyring.OverstyringTidslinjeDto
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsettingSykepengegrunnlagDto
import no.nav.helse.spesialist.db.objectMapper
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class PgOverstyringApiDao internal constructor(
    private val dataSource: DataSource,
) : OverstyringApiDao {
    override fun finnOverstyringer(fødselsnummer: String): List<OverstyringDto> =
        sessionOf(dataSource).use { session ->
            session.transaction {
                it.finnTidslinjeoverstyringer(fødselsnummer) +
                    it.finnInntektsoverstyringer(fødselsnummer) +
                    it.finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer) +
                    it.finnArbeidsforholdoverstyringer(fødselsnummer) +
                    it.finnMinimumSykdomsgradsoverstyringer(fødselsnummer)
            }
        }

    private fun TransactionalSession.finnTidslinjeoverstyringer(fødselsnummer: String): List<OverstyringTidslinjeDto> {
        @Language("PostgreSQL")
        val finnOverstyringQuery = """
            SELECT o.id, o.tidspunkt, o.person_ref, o.hendelse_ref, o.saksbehandler_ref, o.ekstern_hendelse_id, 
            o.ferdigstilt, ot.id AS overstyring_tidslinje_id, ot.arbeidsgiver_identifikator, ot.begrunnelse, 
            p.fødselsnummer, s.navn, s.ident, o.vedtaksperiode_id 
            FROM overstyring o
                INNER JOIN overstyring_tidslinje ot ON ot.overstyring_ref = o.id
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
            WHERE p.fødselsnummer = ? 
        """
        return this.run(
            queryOf(finnOverstyringQuery, fødselsnummer)
                .map { overstyringRow ->
                    val id = overstyringRow.long("overstyring_tidslinje_id")
                    OverstyringTidslinjeDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.string("fødselsnummer"),
                        organisasjonsnummer = overstyringRow.string("arbeidsgiver_identifikator"),
                        vedtaksperiodeId = overstyringRow.uuid("vedtaksperiode_id"),
                        begrunnelse = overstyringRow.string("begrunnelse"),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
                        overstyrteDager =
                            this.run(
                                queryOf(
                                    "SELECT * FROM overstyring_dag WHERE overstyring_tidslinje_ref = ?",
                                    id,
                                ).map { overstyringDagRow ->
                                    OverstyringDagDto(
                                        dato = overstyringDagRow.localDate("dato"),
                                        type = enumValueOf(overstyringDagRow.string("dagtype")),
                                        fraType =
                                            overstyringDagRow.stringOrNull("fra_dagtype")?.let {
                                                enumValueOf<Dagtype>(it)
                                            },
                                        grad = overstyringDagRow.intOrNull("grad"),
                                        fraGrad = overstyringDagRow.intOrNull("fra_grad"),
                                    )
                                }.asList,
                            ),
                    )
                }.asList,
        )
    }

    private fun TransactionalSession.finnInntektsoverstyringer(fødselsnummer: String): List<OverstyringInntektDto> {
        @Language("PostgreSQL")
        val finnOverstyringQuery = """
            SELECT o.id, o.tidspunkt, o.person_ref, o.hendelse_ref, o.saksbehandler_ref, o.ekstern_hendelse_id, 
            o.ferdigstilt, oi.*, p.fødselsnummer, s.navn, s.ident, o.vedtaksperiode_id FROM overstyring o
                INNER JOIN overstyring_inntekt oi ON o.id = oi.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
            WHERE p.fødselsnummer = ?
        """
        return this.run(
            queryOf(finnOverstyringQuery, fødselsnummer)
                .map { overstyringRow ->
                    OverstyringInntektDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.string("fødselsnummer"),
                        organisasjonsnummer = overstyringRow.string("arbeidsgiver_identifikator"),
                        begrunnelse = overstyringRow.string("begrunnelse"),
                        forklaring = overstyringRow.string("forklaring"),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        månedligInntekt = overstyringRow.double("manedlig_inntekt"),
                        fraMånedligInntekt = overstyringRow.doubleOrNull("fra_manedlig_inntekt"),
                        skjæringstidspunkt = overstyringRow.localDate("skjaeringstidspunkt"),
                        refusjonsopplysninger =
                            overstyringRow
                                .stringOrNull("refusjonsopplysninger")
                                ?.let { objectMapper.readValue<List<OverstyringInntektDto.Refusjonselement>>(it) },
                        fraRefusjonsopplysninger =
                            overstyringRow
                                .stringOrNull("fra_refusjonsopplysninger")
                                ?.let { objectMapper.readValue<List<OverstyringInntektDto.Refusjonselement>>(it) },
                        fom = overstyringRow.localDateOrNull("fom"),
                        tom = overstyringRow.localDateOrNull("tom"),
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
                        vedtaksperiodeId = overstyringRow.uuid("vedtaksperiode_id"),
                    )
                }.asList,
        )
    }

    private fun TransactionalSession.finnSkjønnsfastsatteSykepengegrunnlag(
        fødselsnummer: String,
    ): List<SkjønnsfastsettingSykepengegrunnlagDto> {
        @Language("PostgreSQL")
        val finnSkjønnsfastsettingQuery = """
                SELECT o.id, o.tidspunkt, o.person_ref, o.hendelse_ref, o.saksbehandler_ref, o.ekstern_hendelse_id, 
                o.ferdigstilt, o.vedtaksperiode_id, ss.arsak, ss.type, ss.skjaeringstidspunkt, ssa.arlig, ssa.fra_arlig, ssa.arbeidsgiver_identifikator, 
                b1.tekst as fritekst, b2.tekst as mal, b3.tekst as konklusjon, p.fødselsnummer, s.navn, s.ident 
                FROM overstyring o
                    INNER JOIN skjonnsfastsetting_sykepengegrunnlag ss ON o.id = ss.overstyring_ref
                    INNER JOIN skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver ssa ON ssa.skjonnsfastsetting_sykepengegrunnlag_ref = ss.id
                    INNER JOIN person p ON p.id = o.person_ref
                    INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                    INNER JOIN begrunnelse b1 ON ss.begrunnelse_fritekst_ref = b1.id 
                    INNER JOIN begrunnelse b2 ON ss.begrunnelse_mal_ref = b2.id 
                    INNER JOIN begrunnelse b3 ON ss.begrunnelse_konklusjon_ref = b3.id 
                WHERE p.fødselsnummer = ?
            """
        return this.run(
            queryOf(finnSkjønnsfastsettingQuery, fødselsnummer)
                .map { overstyringRow ->
                    SkjønnsfastsettingSykepengegrunnlagDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.string("fødselsnummer"),
                        organisasjonsnummer = overstyringRow.string("arbeidsgiver_identifikator"),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        skjæringstidspunkt = overstyringRow.localDate("skjaeringstidspunkt"),
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
                        vedtaksperiodeId = overstyringRow.uuid("vedtaksperiode_id"),
                        årlig = overstyringRow.double("arlig"),
                        fraÅrlig = overstyringRow.doubleOrNull("fra_arlig"),
                        årsak = overstyringRow.string("arsak"),
                        type = enumValueOf(overstyringRow.string("type").replace("Å", "A")),
                        begrunnelse =
                            overstyringRow.string("mal") + "\n\n" + overstyringRow.string("fritekst") + "\n\n" +
                                overstyringRow.string("konklusjon"),
                        begrunnelseMal = overstyringRow.string("mal"),
                        begrunnelseFritekst = overstyringRow.string("fritekst"),
                        begrunnelseKonklusjon = overstyringRow.string("konklusjon"),
                    )
                }.asList,
        )
    }

    private fun TransactionalSession.finnMinimumSykdomsgradsoverstyringer(fødselsnummer: String): List<OverstyringMinimumSykdomsgradDto> {
        @Language("PostgreSQL")
        val finnOverstyringMinimumSykdomsgradQuery = """
                SELECT o.id, o.tidspunkt, o.person_ref, o.hendelse_ref, o.saksbehandler_ref, o.ekstern_hendelse_id, 
                o.ferdigstilt, o.vedtaksperiode_id, oms.id as overstyring_minimum_sykdomsgrad_ref, oms.fom, oms.tom, oms.vurdering, oms.begrunnelse, 
                omsa.berort_vedtaksperiode_id, omsa.arbeidsgiver_identifikator, p.fødselsnummer, s.navn, s.ident 
                FROM overstyring o
                    INNER JOIN overstyring_minimum_sykdomsgrad oms ON o.id = oms.overstyring_ref
                    INNER JOIN overstyring_minimum_sykdomsgrad_arbeidsgiver omsa ON omsa.overstyring_minimum_sykdomsgrad_ref = oms.id
                    INNER JOIN person p ON p.id = o.person_ref
                    INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                WHERE p.fødselsnummer = ?
            """
        return this.run(
            queryOf(finnOverstyringMinimumSykdomsgradQuery, fødselsnummer)
                .map { overstyringRow ->
                    @Language("PostgreSQL")
                    val finnPerioder = """
                        SELECT fom, tom, vurdering 
                        FROM overstyring_minimum_sykdomsgrad_periode 
                        WHERE overstyring_minimum_sykdomsgrad_ref = ?
                    """
                    val perioderVurdertOk =
                        mutableListOf<OverstyringMinimumSykdomsgradDto.OverstyringMinimumSykdomsgradPeriodeDto>()
                    val perioderVurdertIkkeOk =
                        mutableListOf<OverstyringMinimumSykdomsgradDto.OverstyringMinimumSykdomsgradPeriodeDto>()
                    this.run(
                        queryOf(
                            finnPerioder,
                            overstyringRow.long("overstyring_minimum_sykdomsgrad_ref"),
                        ).map { vurdertPeriode ->
                            val periode =
                                OverstyringMinimumSykdomsgradDto.OverstyringMinimumSykdomsgradPeriodeDto(
                                    fom = vurdertPeriode.localDate("fom"),
                                    tom = vurdertPeriode.localDate("tom"),
                                )
                            if (vurdertPeriode.boolean("vurdering")) {
                                perioderVurdertOk.add(periode)
                            } else {
                                perioderVurdertIkkeOk.add(periode)
                            }
                        }.asList,
                    )
                    OverstyringMinimumSykdomsgradDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.string("fødselsnummer"),
                        organisasjonsnummer = overstyringRow.string("arbeidsgiver_identifikator"),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        perioderVurdertOk = perioderVurdertOk,
                        perioderVurdertIkkeOk = perioderVurdertIkkeOk,
                        begrunnelse = overstyringRow.string("begrunnelse"),
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
                        vedtaksperiodeId = overstyringRow.uuid("vedtaksperiode_id"),
                    )
                }.asList,
        )
    }

    private fun TransactionalSession.finnArbeidsforholdoverstyringer(fødselsnummer: String): List<OverstyringArbeidsforholdDto> {
        @Language("PostgreSQL")
        val finnOverstyringQuery = """
                SELECT o.id, o.tidspunkt, o.person_ref, o.hendelse_ref, o.saksbehandler_ref, o.ekstern_hendelse_id, 
                o.ferdigstilt, oa.*, p.fødselsnummer, o.vedtaksperiode_id, s.navn, s.ident FROM overstyring o 
                    INNER JOIN overstyring_arbeidsforhold oa ON o.id = oa.overstyring_ref
                    INNER JOIN person p ON p.id = o.person_ref
                    INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                WHERE p.fødselsnummer = ?
            """
        return this.run(
            queryOf(finnOverstyringQuery, fødselsnummer)
                .map { overstyringRow ->
                    OverstyringArbeidsforholdDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.string("fødselsnummer"),
                        organisasjonsnummer = overstyringRow.string("arbeidsgiver_identifikator"),
                        begrunnelse = overstyringRow.string("begrunnelse"),
                        forklaring = overstyringRow.string("forklaring"),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        deaktivert = overstyringRow.boolean("deaktivert"),
                        skjæringstidspunkt = overstyringRow.localDate("skjaeringstidspunkt"),
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
                        vedtaksperiodeId = overstyringRow.uuid("vedtaksperiode_id"),
                    )
                }.asList,
        )
    }
}
