package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.TransactionalSession
import no.nav.helse.HelseDao
import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.sykefraværstilfelle.Skjønnsfastsettingsårsak
import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastsettingsårsakDto
import no.nav.helse.objectMapper
import java.time.LocalDate
import javax.sql.DataSource

internal class SykefraværstilfelleDao(dataSource: DataSource) : HelseDao(dataSource) {
    internal fun finnSkjønnsfastsatteSykepengegrunnlag(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): List<SkjønnsfastsattSykepengegrunnlag> {
        return asSQL(
            """
                SELECT ss.type, ss.subsumsjon, ss.skjaeringstidspunkt, fritekst.tekst as fritekst, mal.tekst as mal, konklusjon.tekst as konklusjon, o.tidspunkt FROM begrunnelse fritekst
                JOIN skjonnsfastsetting_sykepengegrunnlag ss ON fritekst.id = ss.begrunnelse_fritekst_ref
                JOIN begrunnelse mal ON mal.id = ss.begrunnelse_mal_ref
                JOIN begrunnelse konklusjon ON konklusjon.id = ss.begrunnelse_konklusjon_ref
                JOIN overstyring o ON ss.overstyring_ref = o.id
                JOIN person ON o.person_ref = person.id
                WHERE ss.skjaeringstidspunkt::date = :skjaeringstidspunkt
                AND fodselsnummer = :fodselsnummer
        """,
            mapOf("skjaeringstidspunkt" to skjæringstidspunkt, "fodselsnummer" to fødselsnummer.toLong()),
        ).list {
            SkjønnsfastsattSykepengegrunnlag(
                type = enumValueOf(it.string("type")),
                årsak =
                    it.string("subsumsjon")
                        .let { objectMapper.readValue<LovhjemmelForDatabase>(it) }.ledd!!.tilÅrsak(),
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                begrunnelseFraMal = it.string("mal"),
                begrunnelseFraFritekst = it.string("fritekst"),
                begrunnelseFraKonklusjon = it.string("konklusjon"),
                opprettet = it.localDateTime("tidspunkt"),
            )
        }
    }

    internal fun TransactionalSession.finnSkjønnsfastsatteSykepengegrunnlag(
        fødselsnummer: String,
    ): List<SkjønnsfastsattSykepengegrunnlagDto> =
        asSQL(
            """
            SELECT ss.type, ss.subsumsjon, ss.skjaeringstidspunkt, fritekst.tekst as fritekst, mal.tekst as mal, konklusjon.tekst as konklusjon, o.tidspunkt FROM begrunnelse fritekst
            JOIN skjonnsfastsetting_sykepengegrunnlag ss ON fritekst.id = ss.begrunnelse_fritekst_ref
            JOIN begrunnelse mal ON mal.id = ss.begrunnelse_mal_ref
            JOIN begrunnelse konklusjon ON konklusjon.id = ss.begrunnelse_konklusjon_ref
            JOIN overstyring o ON ss.overstyring_ref = o.id
            JOIN person ON o.person_ref = person.id
            WHERE fodselsnummer = :fodselsnummer
    """,
            mapOf("fodselsnummer" to fødselsnummer.toLong()),
        ).list(this) {
            SkjønnsfastsattSykepengegrunnlagDto(
                type = enumValueOf(it.string("type")),
                årsak =
                    it.string("subsumsjon")
                        .let { objectMapper.readValue<LovhjemmelForDatabase>(it) }.ledd!!.tilÅrsakDto(),
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                begrunnelseFraMal = it.string("mal"),
                begrunnelseFraFritekst = it.string("fritekst"),
                begrunnelseFraKonklusjon = it.string("konklusjon"),
                opprettet = it.localDateTime("tidspunkt"),
            )
        }
}

private fun String.tilÅrsak(): Skjønnsfastsettingsårsak {
    return when (this) {
        "2" -> Skjønnsfastsettingsårsak.ANDRE_AVSNITT
        "3" -> Skjønnsfastsettingsårsak.TREDJE_AVSNITT
        else -> Skjønnsfastsettingsårsak.ANDRE_AVSNITT
    }
}

private fun String.tilÅrsakDto(): SkjønnsfastsettingsårsakDto {
    return when (this) {
        "2" -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
        "3" -> SkjønnsfastsettingsårsakDto.TREDJE_AVSNITT
        else -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
    }
}
