package no.nav.helse.modell.stoppknapp

import DatabaseIntegrationTest
import no.nav.helse.db.UnntaFraAutomatiseringDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit.SECONDS

internal class UnntaFraAutomatiseringDaoTest : DatabaseIntegrationTest() {
    val dao = UnntaFraAutomatiseringDao(dataSource)

    @Test
    fun `kan lagre status`() {
        val fnr = FNR
        opprettPerson()

        dao.lagre(fnr, true, emptySet())

        assertTrue(SECONDS.between(now(), data<LocalDateTime>(fnr, "oppdatert")) < 5)
        assertTrue(data<Boolean>(fnr, "unnta"))
    }

    @Test
    fun `kan oppdatere status`() {
        val fnr = FNR
        opprettPerson()
        dao.lagre(fnr, true, emptySet())
        assertEquals(emptySet<String>(), data<Set<String>>(fnr, "årsaker"))

        val årsaker = setOf("må stoppes")
        dao.lagre(fnr, true, årsaker)

        assertEquals(årsaker, data<Set<String>>(fnr, "årsaker"))
    }

    @Test
    fun `upsert oppdaterer oppdatert-kolonnen`() {
        val fnr = FNR
        opprettPerson()

        dao.lagre(fnr, true, emptySet())

        val initiellOppdatert = data<LocalDateTime>(fnr, "oppdatert")

        dao.lagre(fnr, false, emptySet())
        assertTrue(initiellOppdatert < data<LocalDateTime>(fnr, "oppdatert"))
    }

    @Test
    fun `finner sist oppdatert-tidspunkt`() {
        val fnr = FNR
        opprettPerson()
        dao.lagre(fnr, true, emptySet())

        val sistOppdatert = dao.sistOppdatert(fnr)

        assertEquals(data<LocalDateTime>(fnr, "oppdatert"), sistOppdatert)
    }

    private inline fun <reified T> data(
        fnr: String,
        kolonne: String,
    ): T =
        query(
            "select $kolonne from unnta_fra_automatisk_godkjenning where fødselsnummer = :fnr",
            "fnr" to fnr.toLong(),
        ).single { row ->
            when (T::class) {
                Set::class -> row.array<String>(1).filterNot(String::isBlank).map { it.replace("'", "") }.toSet() as T
                Boolean::class -> row.boolean(1) as T
                LocalDateTime::class -> row.localDateTime(1) as T

                else -> error("Mangler mapping for ${T::class}")
            }
        }!!
}
