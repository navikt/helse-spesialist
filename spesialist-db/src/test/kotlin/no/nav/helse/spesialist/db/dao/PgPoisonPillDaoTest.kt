package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class PgPoisonPillDaoTest : AbstractDBIntegrationTest() {
    private val poisonPillDao = daos.poisonPillDao

    @Test
    fun `kan finne poison pills`() {
        val poisonPill1 = "key1" to "redpill"
        val poisonPill2 = "key1" to "yellowpill"
        val poisonPill3 = "key2" to "bluepill"
        val ikkePoisonPill1 = "key1" to "candyfloss"
        val ikkePoisonPill2 = "key3" to "redpill"

        insertPoisonPills(poisonPill1, poisonPill2, poisonPill3)

        val poisonPills = poisonPillDao.poisonPills()
        assertTrue(poisonPills.erPoisonPill(poisonPill1.somJsonNode()))
        assertTrue(poisonPills.erPoisonPill(poisonPill2.somJsonNode()))
        assertFalse(poisonPills.erPoisonPill(ikkePoisonPill1.somJsonNode()))
        assertFalse(poisonPills.erPoisonPill(ikkePoisonPill2.somJsonNode()))
    }

    private fun Pair<String, String>.somJsonNode() = objectMapper.valueToTree<JsonNode>(mapOf(first to second))

    private fun insertPoisonPills(vararg meldinger: Pair<String, String>) =
        meldinger.forEach { (feltnavn, identifikator) ->
            dbQuery.update(
                "INSERT INTO poison_pill(feltnavn, identifikator) VALUES (:feltnavn, :identifikator)",
                "feltnavn" to feltnavn,
                "identifikator" to identifikator,
            )
        }
}
