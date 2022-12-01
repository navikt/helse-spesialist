package no.nav.helse.spesialist.api.person

import javax.sql.DataSource
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto

class PersonApiDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun finnEnhet(fødselsnummer: String) = requireNotNull(
        """SELECT id, navn from enhet WHERE id = (SELECT enhet_ref FROM person where fodselsnummer = :fodselsnummer);"""
            .single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { row ->
                EnhetDto(
                    row.string("id"),
                    row.string("navn")
                )
            })

    fun finnInfotrygdutbetalinger(fødselsnummer: String) =
        """ SELECT data FROM infotrygdutbetalinger
            WHERE id = (SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer = :fodselsnummer);
        """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { row -> row.string("data") }

    fun finnesPersonMedFødselsnummer(fødselsnummer: String) =
        """ SELECT 1 FROM person WHERE fodselsnummer = :fodselsnummer """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { true }
            ?: false

    fun personHarAdressebeskyttelse(fødselsnummer: String, adressebeskyttelse: Adressebeskyttelse) =
        """SELECT 1 FROM person p JOIN person_info pi ON p.info_ref = pi.id
            WHERE p.fodselsnummer = :fodselsnummer
            AND pi.adressebeskyttelse = '${adressebeskyttelse.name}'
        """.list(mapOf("fodselsnummer" to fødselsnummer.toLong())) { it }.isNotEmpty()

    fun finnFødselsnummer(aktørId: Long): String? =
        """SELECT fodselsnummer FROM person WHERE aktor_id = :aktor_id;""".single(mapOf("aktor_id" to aktørId)) {
            it.string(
                "fodselsnummer"
            ).padStart(11, '0')
        }

    // Alle peridoer som enten har ført til manuell oppgave eller blitt automatisk godkjent, vil ha et innslag i
    // automatisering-tabellen.
    fun spesialistHarPersonKlarForVisningISpeil(fødselsnummer: String) =
        """SELECT 1 FROM person p
           INNER JOIN vedtak v ON v.person_ref = p.id
           INNER JOIN automatisering a on a.vedtaksperiode_ref = v.id
           WHERE p.fodselsnummer = :fodselsnummer
           LIMIT 1
       """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { true } ?: false
}
