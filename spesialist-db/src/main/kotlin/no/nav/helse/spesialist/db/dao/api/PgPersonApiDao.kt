package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import java.time.LocalDateTime
import javax.sql.DataSource

class PgPersonApiDao internal constructor(
    dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource),
    PersonApiDao {
    override fun personKlargjøres(fødselsnummer: String) {
        asSQL(
            "INSERT INTO person_klargjores(fødselsnummer, opprettet) VALUES(:fodselsnummer, :opprettet) ON CONFLICT DO NOTHING",
            "fodselsnummer" to fødselsnummer,
            "opprettet" to LocalDateTime.now(),
        ).update()
    }

    override fun klargjøringPågår(fødselsnummer: String): Boolean =
        asSQL(
            "SELECT true FROM person_klargjores WHERE fødselsnummer = :fodselsnummer",
            "fodselsnummer" to fødselsnummer,
            "opprettet" to LocalDateTime.now(),
        ).singleOrNull { it.boolean(1) } ?: false

    override fun finnEnhet(fødselsnummer: String) =
        asSQL(
            " SELECT id, navn from enhet WHERE id = (SELECT enhet_ref FROM person where fødselsnummer = :fodselsnummer); ",
            "fodselsnummer" to fødselsnummer,
        ).single { row -> EnhetDto(row.string("id"), row.string("navn")) }

    override fun finnInfotrygdutbetalinger(fødselsnummer: String) =
        asSQL(
            """ SELECT data FROM infotrygdutbetalinger
            WHERE id = (SELECT infotrygdutbetalinger_ref FROM person WHERE fødselsnummer = :fodselsnummer);
        """,
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { row -> row.string("data") }

    override fun finnesPersonMedFødselsnummer(fødselsnummer: String) =
        asSQL(
            " SELECT 1 FROM person WHERE fødselsnummer = :fodselsnummer; ",
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { true } ?: false

    override fun hentAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse? =
        asSQL(
            """
            SELECT pi.adressebeskyttelse
            FROM person p
            JOIN person_info pi ON p.info_ref = pi.id
            WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { Adressebeskyttelse.valueOf(it.string("adressebeskyttelse")) }

    override fun finnAktørId(fødselsnummer: String): String =
        asSQL(
            "SELECT aktør_id FROM person WHERE fødselsnummer = :fodselsnummer",
            "fodselsnummer" to fødselsnummer,
        ).single { it.string("aktør_id") }

    override fun finnFødselsnumre(aktørId: String): List<String> =
        asSQL(
            " SELECT fødselsnummer FROM person WHERE aktør_id = :aktor_id; ",
            "aktor_id" to aktørId,
        ).list { it.string("fødselsnummer") }

    override fun harDataNødvendigForVisning(fødselsnummer: String) =
        asSQL(
            """
            select 1
            from person p
            join person_info pi on p.info_ref = pi.id
            join egen_ansatt ea on ea.person_ref = p.id
            join enhet e on p.enhet_ref = e.id
            where p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { true } ?: false
}
