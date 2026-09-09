package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import no.nav.helse.db.PoisonPillDao
import no.nav.helse.e2e.AbstractDatabaseTest
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class MeldingMediatorTest : AbstractDatabaseTest() {
    private val testRapid = TestRapid()
    private val kommandofabrikk = mockk<Kommandofabrikk>(relaxed = true)
    private val poisonPills: MutableMap<String, Set<String>> = mutableMapOf()
    private val poisonPillDao =
        object : PoisonPillDao {
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
            poisonPillDao = poisonPillDao,
            ignorerMeldingerForUkjentePersoner = false,
            poisonPillTimeToLive = Duration.ofMillis(25),
            versjonAvKode = "1.0.0",
        )

    @BeforeEach
    fun resetTestSetup() {
        testRapid.reset()
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
}
