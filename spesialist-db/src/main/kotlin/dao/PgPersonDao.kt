package no.nav.helse.spesialist.db.dao

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.Adressebeskyttelse
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import java.time.LocalDate
import javax.sql.DataSource

class PgPersonDao internal constructor(
    queryRunner: QueryRunner,
) : PersonDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto? =
        asSQL(
            "SELECT aktør_id, fødselsnummer FROM person WHERE fødselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull { row ->
            MinimalPersonDto(
                aktørId = row.string("aktør_id"),
                fødselsnummer = row.string("fødselsnummer"),
            )
        }

    override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long? =
        asSQL(
            "SELECT id FROM person WHERE fødselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull { row -> row.long("id") }

    override fun finnAdressebeskyttelse(fødselsnummer: String) =
        asSQL(
            """
            SELECT adressebeskyttelse FROM person_info
            WHERE id = (SELECT info_ref FROM person WHERE fødselsnummer = :foedselsnummer);
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull { row -> Adressebeskyttelse.valueOf(row.string("adressebeskyttelse")) }

    override fun finnInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ) = asSQL(
        """
        SELECT inntekter FROM inntekt 
        WHERE person_ref = (SELECT id FROM person WHERE fødselsnummer = :foedselsnummer)
        AND skjaeringstidspunkt = :skjaeringstidspunkt;
        """.trimIndent(),
        "foedselsnummer" to fødselsnummer,
        "skjaeringstidspunkt" to skjæringstidspunkt,
    ).singleOrNull { objectMapper.readValue<List<Inntekter>>(it.string("inntekter")) }

    override fun lagreInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ) = finnPersonMedFødselsnummer(fødselsnummer)?.also {
        asSQL(
            """
            INSERT INTO inntekt (person_ref, skjaeringstidspunkt, inntekter)
            VALUES (:person_ref, :skjaeringstidspunkt, :inntekter::json)
            """.trimIndent(),
            "person_ref" to it,
            "skjaeringstidspunkt" to skjæringstidspunkt,
            "inntekter" to objectMapper.writeValueAsString(inntekter),
        ).update()
    }

    override fun finnEnhetId(fødselsnummer: String): String =
        asSQL(
            "SELECT enhet_ref FROM person where fødselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull {
            it.int("enhet_ref").toEnhetnummer()
        }.let { checkNotNull(it) }

    private fun Int.toEnhetnummer() = if (this < 1000) "0$this" else "$this"
}
