package no.nav.helse.db

import DatabaseIntegrationTest
import kotliquery.sessionOf
import no.nav.helse.modell.kommando.MinimalPersonDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now

internal class TransactionalPersonDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `oppretter minimal person`() {
        kjørTransaksjonelt { dao ->
            dao.lagreMinimalPerson(MinimalPersonDto(FNR, AKTØR))
            val minimalPerson = dao.finnMinimalPerson(FNR)
            assertNotNull(minimalPerson)
            assertEquals(FNR, minimalPerson?.fødselsnummer)
            assertEquals(AKTØR, minimalPerson?.aktørId)
        }
    }

    @Test
    fun `kan fjerne en person fra klargjøringstabellen`() {
        leggInnPersonIKlargjøringstabellen(FNR)
        assertTrue(personFinnesIKlargjøringstabellen(FNR))
        kjørTransaksjonelt { dao -> dao.personKlargjort(FNR) }
        assertFalse(personFinnesIKlargjøringstabellen(FNR))
    }

    private fun kjørTransaksjonelt(block: (TransactionalPersonDao) -> Unit) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                val dao = TransactionalPersonDao(transaction)
                block(dao)
            }
        }
    }

    private fun leggInnPersonIKlargjøringstabellen(fødselsnummer: String) {
        query(
            "INSERT INTO person_klargjores (fødselsnummer, opprettet) VALUES (:fodselsnummer, :opprettet)",
            "fodselsnummer" to fødselsnummer,
            "opprettet" to now()
        ).update()
    }

    private fun personFinnesIKlargjøringstabellen(fødselsnummer: String): Boolean {
        return query(
            "select 1 from person_klargjores where fødselsnummer = :fodselsnummer",
            "fodselsnummer" to fødselsnummer
        ).single { true } ?: false
    }

}
