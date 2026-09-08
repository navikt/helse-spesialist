package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.BegrunnelseForSkjønnsfastsettelseRepository
import no.nav.helse.modell.vedtak.BegrunnelseForSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.Skjønnsfastsettingstype
import no.nav.helse.modell.vedtak.Skjønnsfastsettingsårsak
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.list
import no.nav.helse.spesialist.db.objectMapper
import no.nav.helse.spesialist.domain.Identitetsnummer
import tools.jackson.module.kotlin.readValue

class PgBegrunnelseForSkjønnsfastsettelseRepository internal constructor(
    private val session: Session,
) : BegrunnelseForSkjønnsfastsettelseRepository {
    override fun finnBegrunnelseForSkjønnsfastsattSykepengegrunnlag(identitetsnummer: Identitetsnummer): List<BegrunnelseForSkjønnsfastsattSykepengegrunnlag> =
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
            "fodselsnummer" to identitetsnummer.value,
        ).list(session) {
            BegrunnelseForSkjønnsfastsattSykepengegrunnlag(
                type =
                    when (it.string("type")) {
                        "OMREGNET_ÅRSINNTEKT" -> Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                        "RAPPORTERT_ÅRSINNTEKT" -> Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                        "ANNET" -> Skjønnsfastsettingstype.ANNET
                        else -> error("Ukjent skjønnsfastsettingstype: ${it.string("type")}")
                    },
                årsak =
                    it
                        .string("subsumsjon")
                        .let { objectMapper.readValue<LovhjemmelForDatabase>(it) }
                        .ledd!!
                        .tilÅrsak(),
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                begrunnelseFraMal = it.string("mal"),
                begrunnelseFraFritekst = it.string("fritekst"),
                begrunnelseFraKonklusjon = it.string("konklusjon"),
                opprettet = it.localDateTime("tidspunkt"),
            )
        }

    private fun String.tilÅrsak(): Skjønnsfastsettingsårsak =
        when (this) {
            "2" -> Skjønnsfastsettingsårsak.ANDRE_AVSNITT
            "3" -> Skjønnsfastsettingsårsak.TREDJE_AVSNITT
            else -> Skjønnsfastsettingsårsak.ANDRE_AVSNITT
        }
}

private data class LovhjemmelForDatabase(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
)
