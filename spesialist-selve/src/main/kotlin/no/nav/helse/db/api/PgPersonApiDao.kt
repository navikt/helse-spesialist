package no.nav.helse.db.api

import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import java.time.LocalDateTime
import javax.sql.DataSource

class PgPersonApiDao(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource), PersonApiDao {
    override fun personKlargjøres(fødselsnummer: String) {
        asSQL(
            "INSERT INTO person_klargjores(fødselsnummer, opprettet) VALUES(:fodselsnummer, :opprettet) ON CONFLICT DO NOTHING",
            "fodselsnummer" to fødselsnummer,
            "opprettet" to LocalDateTime.now(),
        ).update()
    }

    override fun klargjøringPågår(fødselsnummer: String): Boolean {
        return asSQL(
            "SELECT true FROM person_klargjores WHERE fødselsnummer = :fodselsnummer",
            "fodselsnummer" to fødselsnummer, "opprettet" to LocalDateTime.now(),
        ).singleOrNull { it.boolean(1) } ?: false
    }

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

    override fun personHarAdressebeskyttelse(
        fødselsnummer: String,
        adressebeskyttelse: Adressebeskyttelse,
    ) = asSQL(
        """SELECT 1 FROM person p JOIN person_info pi ON p.info_ref = pi.id
            WHERE p.fødselsnummer = :fodselsnummer
            AND pi.adressebeskyttelse = '${adressebeskyttelse.name}'
        """,
        "fodselsnummer" to fødselsnummer,
    ).list { it }.isNotEmpty()

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
            left join person_info pi on p.info_ref = pi.id
            left join egen_ansatt ea on ea.person_ref = p.id
            left join enhet e on p.enhet_ref = e.id
            where p.fødselsnummer = :fodselsnummer
                and (pi.id is not null and ea.er_egen_ansatt is not null and e.id is not null)
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { true } ?: false
}
