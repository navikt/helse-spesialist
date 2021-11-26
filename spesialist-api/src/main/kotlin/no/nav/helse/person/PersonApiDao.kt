package no.nav.helse.person

import no.nav.helse.HelseDao
import no.nav.helse.vedtaksperiode.EnhetDto
import javax.sql.DataSource

class PersonApiDao(dataSource: DataSource): HelseDao(dataSource) {
    fun finnEnhet(fødselsnummer: String) = requireNotNull(
        """SELECT id, navn from enhet WHERE id = (SELECT enhet_ref FROM person where fodselsnummer = :fodselsnummer);"""
            .single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { row -> EnhetDto(row.string("id"), row.string("navn")) })

    fun finnInfotrygdutbetalinger(fødselsnummer: String) =
        """ SELECT data FROM infotrygdutbetalinger
            WHERE id = (SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer = :fodselsnummer);
        """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { row -> row.string("data") }

    fun finnesPersonMedFødselsnummer(fødselsnummer: String) =
        """ SELECT 1 FROM person WHERE fodselsnummer = :fodselsnummer """.single(mapOf("fodselsnummer" to fødselsnummer.toLong())) { true }  ?: false

    fun personErKode7(fnr: String) =
        """SELECT 1 FROM person p JOIN person_info pi ON p.info_ref = pi.id
            WHERE p.fodselsnummer = :fodselsnummer
            AND pi.adressebeskyttelse = '${Adressebeskyttelse.Fortrolig.name}'
        """.list(mapOf("fodselsnummer" to fnr.toLong())) { it }.isNotEmpty()
}
