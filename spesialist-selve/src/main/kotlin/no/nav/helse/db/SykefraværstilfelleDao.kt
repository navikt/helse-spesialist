package no.nav.helse.db

import java.time.LocalDate
import javax.sql.DataSource
import no.nav.helse.HelseDao
import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastattSykepengegrunnlag

internal class SykefraværstilfelleDao(dataSource: DataSource) : HelseDao(dataSource) {

    internal fun finnSkjønnsfastsatteSykepengegrunnlag(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): List<SkjønnsfastattSykepengegrunnlag> {
        return asSQL(
            """
                SELECT ss.skjaeringstidspunkt, fritekst.tekst as fritekst, mal.tekst as mal, o.tidspunkt FROM begrunnelse fritekst
                JOIN skjonnsfastsetting_sykepengegrunnlag ss ON fritekst.id = ss.begrunnelse_fritekst_ref
                JOIN begrunnelse mal ON mal.id = ss.begrunnelse_mal_ref
                JOIN overstyring o ON ss.overstyring_ref = o.id
                JOIN person ON o.person_ref = person.id
                WHERE ss.skjaeringstidspunkt::date = :skjaeringstidspunkt
                AND fodselsnummer = :fodselsnummer
        """,
            mapOf("skjaeringstidspunkt" to skjæringstidspunkt, "fodselsnummer" to fødselsnummer.toLong())
        ).list {
            SkjønnsfastattSykepengegrunnlag(
                skjæringstidspunkt = it.localDate("skjaeringstidspunkt"),
                begrunnelseFraMal = it.string("mal"),
                begrunnelseFraFritekst = it.string("fritekst"),
                opprettet = it.localDateTime("tidspunkt")
            )
        }
    }
}