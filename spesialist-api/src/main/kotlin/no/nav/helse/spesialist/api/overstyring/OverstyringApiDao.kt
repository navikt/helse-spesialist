package no.nav.helse.spesialist.api.overstyring

import com.fasterxml.jackson.module.kotlin.readValue
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverDto
import no.nav.helse.spesialist.api.graphql.schema.Arbeidsgiver
import no.nav.helse.spesialist.api.objectMapper
import org.intellij.lang.annotations.Language

class OverstyringApiDao(private val dataSource: DataSource) {
    fun finnOverstyringerAvTidslinjer(fødselsnummer: String, organisasjonsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val finnOverstyringQuery = """
            SELECT o.*, p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a ON a.id = o.arbeidsgiver_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
            WHERE p.fodselsnummer = ? 
            AND a.orgnummer = ?
            AND o.id IN (SELECT overstyring_ref FROM overstyring_dag)
        """
        session.run(
            queryOf(finnOverstyringQuery, fødselsnummer.toLong(), organisasjonsnummer.toLong())
                .map { overstyringRow ->
                    val id = overstyringRow.long("id")
                    OverstyringDto(
                        hendelseId = overstyringRow.uuid("hendelse_ref"),
                        fødselsnummer = overstyringRow.long("fodselsnummer").toFødselsnummer(),
                        organisasjonsnummer = overstyringRow.int("orgnummer").toString(),
                        begrunnelse = overstyringRow.string("begrunnelse"),
                        timestamp = overstyringRow.localDateTime("tidspunkt"),
                        saksbehandlerNavn = overstyringRow.string("navn"),
                        saksbehandlerIdent = overstyringRow.stringOrNull("ident"),
                        overstyrteDager = session.run(
                            queryOf(
                                "SELECT * FROM overstyring_dag WHERE overstyring_ref = ?", id
                            ).map { overstyringDagRow ->
                                OverstyringDagDto(
                                    dato = overstyringDagRow.localDate("dato"),
                                    type = enumValueOf(overstyringDagRow.string("dagtype")),
                                    fraType = overstyringDagRow.stringOrNull("fra_dagtype")?.let {
                                        enumValueOf<Dagtype>(it)
                                    },
                                    grad = overstyringDagRow.intOrNull("grad"),
                                    fraGrad = overstyringDagRow.intOrNull("fra_grad")
                                )
                            }.asList
                        )
                    )
                }.asList
        )
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()

    fun finnOverstyringerAvInntekt(fødselsnummer: String, organisasjonsnummer: String): List<OverstyringInntektDto> =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val finnOverstyringQuery = """
            SELECT o.*, oi.*, p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o
                INNER JOIN overstyring_inntekt oi ON o.id = oi.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a ON a.id = o.arbeidsgiver_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                INNER JOIN hendelse h ON h.id = o.hendelse_ref
            WHERE p.fodselsnummer = ? AND a.orgnummer = ?
        """
            session.run(
                queryOf(finnOverstyringQuery, fødselsnummer.toLong(), organisasjonsnummer.toLong())
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
                            refusjonsopplysninger = overstyringRow.stringOrNull("refusjonsopplysninger")
                                ?.let { objectMapper.readValue<List<OverstyringInntektDto.Refusjonselement>>(it) },
                            fraRefusjonsopplysninger = overstyringRow.stringOrNull("fra_refusjonsopplysninger")
                                ?.let { objectMapper.readValue<List<OverstyringInntektDto.Refusjonselement>>(it) },
                        )
                    }.asList
            )
        }

    fun finnOverstyringerAvInntektOgRefusjon(fødselsnummer: String, organisasjonsnummer: String): List<OverstyringInntektDto> =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val finnOverstyringQuery = """
            SELECT o.*, oi.*, p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o
                INNER JOIN overstyring_inntekt oi ON o.id = oi.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a ON a.id = o.arbeidsgiver_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                INNER JOIN hendelse h ON h.id = o.hendelse_ref
            WHERE p.fodselsnummer = ? AND a.orgnummer = ?
        """
            session.run(
                queryOf(finnOverstyringQuery, fødselsnummer.toLong(), organisasjonsnummer.toLong())
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
                            refusjonsopplysninger = overstyringRow.stringOrNull("refusjonsopplysninger")
                                ?.let { objectMapper.readValue<List<OverstyringInntektDto.Refusjonselement>>(it) },
                            fraRefusjonsopplysninger = overstyringRow.stringOrNull("fra_refusjonsopplysninger")
                                ?.let { objectMapper.readValue<List<OverstyringInntektDto.Refusjonselement>>(it) },
                        )
                    }.asList
            )
        }

    fun finnOverstyringerAvArbeidsforhold(
        fødselsnummer: String,
        orgnummer: String
    ): List<OverstyringArbeidsforholdDto> = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val finnOverstyringQuery = """
            SELECT o.*, oa.*, p.fodselsnummer, a.orgnummer, s.navn, s.ident FROM overstyring o 
                INNER JOIN overstyring_arbeidsforhold oa ON o.id = oa.overstyring_ref
                INNER JOIN person p ON p.id = o.person_ref
                INNER JOIN arbeidsgiver a ON a.id = o.arbeidsgiver_ref
                INNER JOIN saksbehandler s ON s.oid = o.saksbehandler_ref
                INNER JOIN hendelse h ON h.id = o.hendelse_ref
            WHERE p.fodselsnummer = ? AND a.orgnummer = ?
        """
        it.run(
            queryOf(finnOverstyringQuery, fødselsnummer.toLong(), orgnummer.toLong())
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
                        skjæringstidspunkt = overstyringRow.localDate("skjaeringstidspunkt")
                    )
                }.asList
        )
    }
}
