package no.nav.helse.spesialist.api.overstyring

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.objectMapper
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class OverstyringApiDao(
    private val dataSource: DataSource,
) {
    fun finnOverstyringer(fødselsnummer: String): List<OverstyringDto> =
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
            o.ferdigstilt, ot.id AS overstyring_tidslinje_id, ot.arbeidsgiver_ref, ot.begrunnelse, p.fodselsnummer, 
            a.orgnummer, s.navn, s.ident, o.vedtaksperiode_id FROM overstyring o
                INNER JOIN overstyring_tidslinje ot ON ot.overstyring_ref = o.id
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a ON a.id = ot.arbeidsgiver_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
            WHERE p.fodselsnummer = ? 
        """
        return this.run(
            queryOf(finnOverstyringQuery, fødselsnummer.toLong())
                .map { overstyringRow ->
                    val id = overstyringRow.long("overstyring_tidslinje_id")
                    OverstyringTidslinjeDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
                        organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
                        vedtaksperiodeId = overstyringRow.uuidOrNull("vedtaksperiode_id"),
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
            o.ferdigstilt, oi.*, p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o
                INNER JOIN overstyring_inntekt oi ON o.id = oi.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a ON a.id = oi.arbeidsgiver_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
            WHERE p.fodselsnummer = ?
        """
        return this.run(
            queryOf(finnOverstyringQuery, fødselsnummer.toLong())
                .map { overstyringRow ->
                    OverstyringInntektDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
                        organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
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
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
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
                o.ferdigstilt, ss.arsak, ss.type, ssa.arlig, ssa.fra_arlig, ss.skjaeringstidspunkt, 
                b1.tekst as fritekst, b2.tekst as mal, b3.tekst as konklusjon, p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o
                    INNER JOIN skjonnsfastsetting_sykepengegrunnlag ss ON o.id = ss.overstyring_ref
                    INNER JOIN skjonnsfastsetting_sykepengegrunnlag_arbeidsgiver ssa ON ssa.skjonnsfastsetting_sykepengegrunnlag_ref = ss.id
                    INNER JOIN person p ON p.id = o.person_ref
                    INNER JOIN arbeidsgiver a ON a.id = ssa.arbeidsgiver_ref
                    INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                    INNER JOIN begrunnelse b1 ON ss.begrunnelse_fritekst_ref = b1.id 
                    INNER JOIN begrunnelse b2 ON ss.begrunnelse_mal_ref = b2.id 
                    INNER JOIN begrunnelse b3 ON ss.begrunnelse_konklusjon_ref = b3.id 
                WHERE p.fodselsnummer = ?
            """
        return this.run(
            queryOf(finnSkjønnsfastsettingQuery, fødselsnummer.toLong())
                .map { overstyringRow ->
                    SkjønnsfastsettingSykepengegrunnlagDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
                        organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        skjæringstidspunkt = overstyringRow.localDate("skjaeringstidspunkt"),
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
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
                o.ferdigstilt, oms.fom, oms.tom, oms.vurdering, oms.begrunnelse, omsa.berort_vedtaksperiode_id, 
                p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o
                    INNER JOIN overstyring_minimum_sykdomsgrad oms ON o.id = oms.overstyring_ref
                    INNER JOIN overstyring_minimum_sykdomsgrad_arbeidsgiver omsa ON omsa.overstyring_minimum_sykdomsgrad_ref = oms.id
                    INNER JOIN person p ON p.id = o.person_ref
                    INNER JOIN arbeidsgiver a ON a.id = omsa.arbeidsgiver_ref
                    INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                WHERE p.fodselsnummer = ?
            """
        return this.run(
            queryOf(finnOverstyringMinimumSykdomsgradQuery, fødselsnummer.toLong())
                .map { overstyringRow ->
                    OverstyringMinimumSykdomsgradDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
                        organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
                        fom = overstyringRow.localDate("fom"),
                        tom = overstyringRow.localDate("tom"),
                        vurdering = overstyringRow.boolean("vurdering"),
                        begrunnelse = overstyringRow.string("begrunnelse"),
                    )
                }.asList,
        )
    }

    private fun TransactionalSession.finnArbeidsforholdoverstyringer(fødselsnummer: String): List<OverstyringArbeidsforholdDto> {
        @Language("PostgreSQL")
        val finnOverstyringQuery = """
                SELECT o.id, o.tidspunkt, o.person_ref, o.hendelse_ref, o.saksbehandler_ref, o.ekstern_hendelse_id, 
                o.ferdigstilt, oa.*, p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o 
                    INNER JOIN overstyring_arbeidsforhold oa ON o.id = oa.overstyring_ref
                    INNER JOIN person p ON p.id = o.person_ref
                    INNER JOIN arbeidsgiver a ON a.id = oa.arbeidsgiver_ref
                    INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                WHERE p.fodselsnummer = ?
            """
        return this.run(
            queryOf(finnOverstyringQuery, fødselsnummer.toLong())
                .map { overstyringRow ->
                    OverstyringArbeidsforholdDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
                        organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
                        begrunnelse = overstyringRow.string("begrunnelse"),
                        forklaring = overstyringRow.string("forklaring"),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        deaktivert = overstyringRow.boolean("deaktivert"),
                        skjæringstidspunkt = overstyringRow.localDate("skjaeringstidspunkt"),
                        ferdigstilt = overstyringRow.boolean("ferdigstilt"),
                    )
                }.asList,
        )
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
