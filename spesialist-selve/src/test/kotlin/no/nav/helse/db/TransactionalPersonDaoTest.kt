package no.nav.helse.db

import DatabaseIntegrationTest
import kotliquery.sessionOf
import no.nav.helse.modell.kommando.MinimalPersonDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class TransactionalPersonDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `oppretter minimal person`() {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                val dao = TransactionalPersonDao(transaction)
                dao.lagreMinimalPerson(MinimalPersonDto(FNR, AKTØR))
                val minimalPerson = dao.finnMinimalPerson(FNR)
                assertNotNull(minimalPerson)
                assertEquals(FNR, minimalPerson?.fødselsnummer)
                assertEquals(AKTØR, minimalPerson?.aktørId)
            }
        }
    }
}
