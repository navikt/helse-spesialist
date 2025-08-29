package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.PoisonPillDao
import no.nav.helse.e2e.AbstractDatabaseTest
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class MeldingMediatorTest : AbstractDatabaseTest() {
    private val testRapid = TestRapid()
    private val kommandofabrikk = mockk<Kommandofabrikk>(relaxed = true)
    private val poisonPills: MutableMap<String, Set<String>> = mutableMapOf()
    private val poisonPillDao = object : PoisonPillDao {
        override fun poisonPills() = PoisonPills(poisonPills.toMap())
    }

    private val meldingMediator =
        MeldingMediator(
            sessionFactory = TransactionalSessionFactory(dataSource),
            personDao = daos.personDao,
            commandContextDao = daos.commandContextDao,
            meldingDao = daos.meldingDao,
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            kommandofabrikk = kommandofabrikk,
            dokumentDao = daos.dokumentDao,
            varselRepository = VarselRepository(
                varselDao = daos.varselDao,
                definisjonDao = daos.definisjonDao
            ),
            poisonPillDao = poisonPillDao,
            ignorerMeldingerForUkjentePersoner = false,
            poisonPillTimeToLive = Duration.ofMillis(50),
            annulleringDao = daos.annulleringDao,
        )

    @BeforeEach
    fun resetTestSetup() {
        testRapid.reset()
        lagVarseldefinisjoner()
    }

    @Test
    fun `lagre varseldefinisjon`() {
        val id = UUID.randomUUID()
        val varseldefinisjon = Varseldefinisjon(id, "SB_EX_1", "En tittel", null, null, false, LocalDateTime.now())
        meldingMediator.håndter(varseldefinisjon)
        assertVarseldefinisjon(id)
    }

    @Test
    fun `hopper over meldinger hvis de er flagget i poison pills`() {
        poisonPills["@id"] = setOf("ekkel id")
        assertFalse(meldingMediator.skalBehandleMelding(""" { "@id": "ekkel id" } """))
        assertTrue(meldingMediator.skalBehandleMelding(""" { "@id": "fin id" } """))
    }

    @Test
    fun `leser nye poison pills etter at cachen går ut`() {
        assertTrue(meldingMediator.skalBehandleMelding(""" { "@id": "ekkel id" } """))
        poisonPills["@id"] = setOf("ekkel id")
        assertTrue(meldingMediator.skalBehandleMelding(""" { "@id": "ekkel id" } """))
        Thread.sleep(60)
        assertFalse(meldingMediator.skalBehandleMelding(""" { "@id": "ekkel id" } """))
    }

    private fun assertVarseldefinisjon(id: UUID) {
        @Language("PostgreSQL")
        val query = " SELECT COUNT(1) FROM api_varseldefinisjon WHERE unik_id = ? "
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, id).map { it.int(1) }.asSingle)
            }
        assertEquals(1, antall)
    }

    private fun lagVarseldefinisjoner() {
        Varselkode.entries.forEach { varselkode ->
            lagVarseldefinisjon(varselkode.name)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet)
            VALUES (:unik_id, :kode, :tittel, :forklaring, :handling, :avviklet, :opprettet)
            ON CONFLICT (unik_id) DO NOTHING
            """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "unik_id" to UUID.nameUUIDFromBytes(varselkode.toByteArray()),
                        "kode" to varselkode,
                        "tittel" to "En tittel for varselkode=$varselkode",
                        "forklaring" to "En forklaring for varselkode=$varselkode",
                        "handling" to "En handling for varselkode=$varselkode",
                        "avviklet" to false,
                        "opprettet" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }
    }
}
