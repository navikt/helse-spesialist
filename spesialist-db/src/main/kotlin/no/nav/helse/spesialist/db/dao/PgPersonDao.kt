package no.nav.helse.spesialist.db.dao

import com.fasterxml.jackson.databind.JsonNode
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

    override fun personKlargjort(fødselsnummer: String) {
        asSQL("DELETE FROM person_klargjores WHERE fødselsnummer = :foedselsnummer", "foedselsnummer" to fødselsnummer).update()
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

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String) =
        asSQL(
            "SELECT infotrygdutbetalinger_oppdatert FROM person WHERE fødselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull { it.localDateOrNull("infotrygdutbetalinger_oppdatert") }

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

    override fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ) = finnInfotrygdutbetalingerRef(fødselsnummer)
        ?.also {
            updateInfotrygdutbetalinger(it, fødselsnummer, utbetalinger)
        }
        ?: insertInfotrygdubetalinger(utbetalinger, fødselsnummer)

    private fun updateInfotrygdutbetalinger(
        infotrygdutbetalingerId: Long,
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ) {
        asSQL(
            """
            UPDATE infotrygdutbetalinger SET data = CAST(:utbetalinger as json)
            WHERE id = :infotrygdutbetalingerId;
            """.trimIndent(),
            "utbetalinger" to objectMapper.writeValueAsString(utbetalinger),
            "infotrygdutbetalingerId" to infotrygdutbetalingerId,
        ).update()

        updateInfotrygdutbetalingerOppdatert(fødselsnummer)
    }

    private fun updateInfotrygdutbetalingerOppdatert(fødselsnummer: String) {
        asSQL(
            "UPDATE person SET infotrygdutbetalinger_oppdatert = now() WHERE fødselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer,
        ).update()
    }

    private fun insertInfotrygdubetalinger(
        utbetalinger: JsonNode,
        fødselsnummer: String,
    ): Long {
        val infotrygdutbetalingerId =
            asSQL(
                "INSERT INTO infotrygdutbetalinger (data) VALUES (CAST(:data as json))",
                "data" to objectMapper.writeValueAsString(utbetalinger),
            ).updateAndReturnGeneratedKey()
        updateInfotrygdutbetalingerRef(infotrygdutbetalingerId, fødselsnummer)
        return infotrygdutbetalingerId
    }

    private fun updateInfotrygdutbetalingerRef(
        infotrygdutbetalingerId: Long,
        fødselsnummer: String,
    ) {
        asSQL(
            """
            UPDATE person SET infotrygdutbetalinger_ref = :infotrygdutbetalingerId, infotrygdutbetalinger_oppdatert = now()
            WHERE fødselsnummer = :foedselsnummer
            """.trimIndent(),
            "infotrygdutbetalingerId" to infotrygdutbetalingerId,
            "foedselsnummer" to fødselsnummer,
        ).update()
    }

    private fun finnInfotrygdutbetalingerRef(fødselsnummer: String) =
        asSQL(
            "SELECT infotrygdutbetalinger_ref FROM person WHERE fødselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull { it.longOrNull("infotrygdutbetalinger_ref") }

    override fun finnEnhetId(fødselsnummer: String): String =
        asSQL(
            "SELECT enhet_ref FROM person where fødselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull {
            it.int("enhet_ref").toEnhetnummer()
        }.let { checkNotNull(it) }

    private fun Int.toEnhetnummer() = if (this < 1000) "0$this" else "$this"
}
