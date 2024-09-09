package no.nav.helse.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PoisonPillDaoTest: AbstractDatabaseTest() {

    private val poisonPillDao = PoisonPillDao(dataSource)

    @Test
    fun `kan finne poison pills`() {
        lagPoisonPill("key1", "redpill")
        lagPoisonPill("key1", "yellowpill")
        lagPoisonPill("key2", "bluepill")

        val poisonPills = poisonPillDao.poisonPills()
        assertEquals(2, poisonPills.size)
        assertEquals(setOf("redpill", "yellowpill"), poisonPills["key1"])
        assertEquals(setOf("bluepill"), poisonPills["key2"])
    }

    private fun lagPoisonPill(feltnavn: String, identifikator: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """ INSERT INTO poison_pill(feltnavn, identifikator) VALUES (?, ?) """
        session.run(queryOf(query, feltnavn, identifikator).asExecute)
    }
}
