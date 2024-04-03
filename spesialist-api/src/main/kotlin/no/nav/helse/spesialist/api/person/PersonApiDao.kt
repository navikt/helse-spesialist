package no.nav.helse.spesialist.api.person

import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import javax.sql.DataSource

class PersonApiDao(dataSource: DataSource) : HelseDao(dataSource) {
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

    // Alle peridoer som enten har ført til manuell oppgave eller blitt automatisk godkjent, vil ha et innslag i
    // automatisering-tabellen.
    fun spesialistHarPersonKlarForVisningISpeil(fødselsnummer: String) =
        asSQL(
            """
            SELECT 1 FROM person p
            INNER JOIN vedtak v ON v.person_ref = p.id
            LEFT JOIN automatisering a ON a.vedtaksperiode_ref = v.id
            LEFT JOIN oppgave o ON o.vedtak_ref = v.id
            WHERE p.fodselsnummer = :fodselsnummer
              AND (a.id IS NOT NULL OR o.id IS NOT NULL)
            LIMIT 1
       """,
            mapOf("fodselsnummer" to fødselsnummer.toLong()),
        ).single { true } ?: false
}
