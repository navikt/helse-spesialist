package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private class PgVergemålApiDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val vergemålApiDao = PgVergemålApiDao(dataSource)

    @Test
    fun `kan hente ut om person har vergemål`() {
        opprettArbeidsgiver()
        opprettVedtaksperiode(fødselsnummer = person.id.value)
        opprettVergemål(person.id.value, harFullmakt = true)

        assertEquals(true, vergemålApiDao.harFullmakt(person.id.value))
    }

    private fun opprettVergemål(
        fødselsnummer: String,
        harVergemål: Boolean = false,
        harFullmakt: Boolean = false,
        harFremtidsFullmakt: Boolean = false,
    ) = dbQuery.update(
        """
        insert into vergemal (person_ref, har_vergemal, har_fremtidsfullmakter, har_fullmakter, vergemål_oppdatert, fullmakt_oppdatert) 
        select id, :harVergemal, :harFremtidsFullmakt, :harFullmakter, :oppdatert, :oppdatert
        from person
        where fødselsnummer = :foedselsnummer
        """.trimIndent(),
        "foedselsnummer" to fødselsnummer,
        "harVergemal" to harVergemål,
        "harFremtidsFullmakt" to harFremtidsFullmakt,
        "harFullmakter" to harFullmakt,
        "oppdatert" to LocalDateTime.now(),
    )
}
