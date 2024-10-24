package no.nav.helse.spesialist.api.person

import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import java.time.LocalDateTime
import javax.sql.DataSource

class PersonApiDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun personKlargjøres(fødselsnummer: String) {
        asSQL(
            "INSERT INTO person_klargjores(fødselsnummer, opprettet) VALUES(:fodselsnummer, :opprettet) ON CONFLICT DO NOTHING",
            mapOf("fodselsnummer" to fødselsnummer, "opprettet" to LocalDateTime.now()),
        ).update()
    }

    fun klargjøringPågår(fødselsnummer: String): Boolean {
        return asSQL(
            "SELECT true FROM person_klargjores WHERE fødselsnummer = :fodselsnummer",
            mapOf("fodselsnummer" to fødselsnummer, "opprettet" to LocalDateTime.now()),
        ).single { it.boolean(1) } ?: false
    }

    fun finnEnhet(fødselsnummer: String) =
        requireNotNull(
            asSQL(
                " SELECT id, navn from enhet WHERE id = (SELECT enhet_ref FROM person where fodselsnummer = :fodselsnummer); ",
                mapOf("fodselsnummer" to fødselsnummer.toLong()),
            ).single { row -> EnhetDto(row.string("id"), row.string("navn")) },
        )

    fun finnInfotrygdutbetalinger(fødselsnummer: String) =
        asSQL(
            """ SELECT data FROM infotrygdutbetalinger
            WHERE id = (SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer = :fodselsnummer);
        """,
            mapOf("fodselsnummer" to fødselsnummer.toLong()),
        ).single { row -> row.string("data") }

    fun finnesPersonMedFødselsnummer(fødselsnummer: String) =
        asSQL(
            " SELECT 1 FROM person WHERE fodselsnummer = :fodselsnummer; ",
            mapOf("fodselsnummer" to fødselsnummer.toLong()),
        ).single { true } ?: false

    fun personHarAdressebeskyttelse(
        fødselsnummer: String,
        adressebeskyttelse: Adressebeskyttelse,
    ) = asSQL(
        """SELECT 1 FROM person p JOIN person_info pi ON p.info_ref = pi.id
            WHERE p.fodselsnummer = :fodselsnummer
            AND pi.adressebeskyttelse = '${adressebeskyttelse.name}'
        """,
        mapOf("fodselsnummer" to fødselsnummer.toLong()),
    ).list { it }.isNotEmpty()

    fun finnAktørId(fødselsnummer: String): String =
        requireNotNull(
            asSQL(
                "SELECT aktor_id FROM person WHERE fodselsnummer = :fodselsnummer",
                mapOf("fodselsnummer" to fødselsnummer.toLong()),
            ).single { it.string("aktor_id") },
        )

    fun finnFødselsnummer(aktørId: Long): String? =
        asSQL(
            " SELECT fodselsnummer FROM person WHERE aktor_id = :aktor_id; ",
            mapOf("aktor_id" to aktørId),
        ).single { it.string("fodselsnummer").padStart(11, '0') }

    fun finnFødselsnumre(aktørId: Long): List<String> =
        asSQL(
            " SELECT fodselsnummer FROM person WHERE aktor_id = :aktor_id; ",
            mapOf("aktor_id" to aktørId),
        ).list { it.string("fodselsnummer").padStart(11, '0') }

    fun harDataNødvendigForVisning(fødselsnummer: String) =
        asSQL(
            """
            select 1
            from person p
            left join person_info pi on p.info_ref = pi.id
            left join egen_ansatt ea on ea.person_ref = p.id
            left join enhet e on p.enhet_ref = e.id
            where p.fodselsnummer = :fodselsnummer
                and (pi.id is not null and ea.er_egen_ansatt is not null and e.id is not null)
            """.trimIndent(),
            mapOf("fodselsnummer" to fødselsnummer.toLong()),
        ).single { true } ?: false
}
