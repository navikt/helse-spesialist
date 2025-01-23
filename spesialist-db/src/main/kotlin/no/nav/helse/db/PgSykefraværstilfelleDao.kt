package no.nav.helse.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.list
import no.nav.helse.db.overstyring.LovhjemmelForDatabase
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingsårsakDto
import no.nav.helse.objectMapper

class PgSykefraværstilfelleDao(private val session: Session) : SykefraværstilfelleDao {
    override fun finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer: String): List<SkjønnsfastsattSykepengegrunnlagDto> =
        asSQL(
            """
            SELECT ss.type, ss.subsumsjon, ss.skjaeringstidspunkt, fritekst.tekst as fritekst, mal.tekst as mal, konklusjon.tekst as konklusjon, o.tidspunkt FROM begrunnelse fritekst
            JOIN skjonnsfastsetting_sykepengegrunnlag ss ON fritekst.id = ss.begrunnelse_fritekst_ref
            JOIN begrunnelse mal ON mal.id = ss.begrunnelse_mal_ref
            JOIN begrunnelse konklusjon ON konklusjon.id = ss.begrunnelse_konklusjon_ref
            JOIN overstyring o ON ss.overstyring_ref = o.id
            JOIN person ON o.person_ref = person.id
            WHERE fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).list(session) {
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

private fun String.tilÅrsakDto(): SkjønnsfastsettingsårsakDto {
    return when (this) {
        "2" -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
        "3" -> SkjønnsfastsettingsårsakDto.TREDJE_AVSNITT
        else -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
    }
}
