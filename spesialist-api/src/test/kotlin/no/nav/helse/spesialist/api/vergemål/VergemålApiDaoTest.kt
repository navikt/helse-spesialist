package no.nav.helse.spesialist.api.vergemål

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

private class VergemålApiDaoTest : DatabaseIntegrationTest() {
    private fun opprettVergemål(
        personId: Long,
        harVergemål: Boolean = false,
        harFullmakt: Boolean = false,
        harFremtidsFullmakt: Boolean = false
    ) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO vergemal(person_ref, har_vergemal, har_fremtidsfullmakter, har_fullmakter, vergemål_oppdatert, fullmakt_oppdatert) 
                VALUES(:personRef, :harVergemal, :harFremtidsFullmakt, :harFullmakter, :oppdatert, :oppdatert)
                """
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "personRef" to personId,
                        "harVergemal" to harVergemål,
                        "harFremtidsFullmakt" to harFremtidsFullmakt,
                        "harFullmakter" to harFullmakt,
                        "oppdatert" to LocalDateTime.now()
                    )
                ).asExecute
            )
        }

    @Test
    fun `kan hente ut om person har vergemål`() {
        val personRef = opprettPerson()
        opprettVedtaksperiode(personRef, opprettArbeidsgiver())
        opprettVergemål(personRef, harFullmakt = true)

        assertEquals(true, vergemålApiDao.harFullmakt(FØDSELSNUMMER))
    }
}