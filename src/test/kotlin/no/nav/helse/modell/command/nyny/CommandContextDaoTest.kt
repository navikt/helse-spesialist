package no.nav.helse.modell.command.nyny

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CommandContextDaoTest : AbstractEndToEndTest() {

    private companion object {
        private const val FNR = "FNR"
        private val VEDTAKSPERIODE1 = UUID.randomUUID()
        private val VEDTAKSPERIODE2 = UUID.randomUUID()
        private val HENDELSE1 = TestHendelse(UUID.randomUUID(), VEDTAKSPERIODE1, FNR)
        private val HENDELSE2 = TestHendelse(UUID.randomUUID(), VEDTAKSPERIODE2, FNR)
    }

    @Test
    fun `lagrer context i db`() {
        val contextId = ny()
        assertTilstand(contextId, "NY")
    }

    @Test
    fun `finner suspendert context i db`() {
        val contextId = suspendert()
        assertNotNull(commandContextDao.finn(contextId))
        assertTilstand(contextId, "NY", "SUSPENDERT")
    }

    @Test
    fun `avbryter ikke seg selv`() {
        val contextId = ny()
        avbryt(contextId)
        assertTilstand(contextId, "NY")
    }

    @Test
    fun `avbryter command som er NY eller SUSPENDERT`() {
        val contextId1 = UUID.randomUUID()
        val contextId2 = ny()
        val contextId3 = suspendert()
        val contextId4 = ferdig()
        avbryt(contextId1)
        assertTilstand(contextId2, "NY", "AVBRUTT")
        assertTilstand(contextId3, "NY", "SUSPENDERT", "AVBRUTT")
        assertTilstand(contextId4, "NY", "FERDIG")
    }

    @Test
    fun `avbryter ikke commands som er FEIL eller FERDIG`() {
        val contextId1 = UUID.randomUUID()
        val contextId2 = ferdig()
        val contextId3 = feil()
        val contextId4 = ny()
        avbryt(contextId1)
        assertTilstand(contextId2, "NY", "FERDIG")
        assertTilstand(contextId3, "NY", "FEIL")
        assertTilstand(contextId4, "NY", "AVBRUTT")
    }

    @Test
    fun `avbryter bare for riktig vedtaksperiode`() {
        val contextId1 = ny(HENDELSE1)
        val contextId2 = ny(HENDELSE2)
        avbryt(UUID.randomUUID(), HENDELSE1.vedtaksperiodeId())
        assertTilstand(contextId1, "NY", "AVBRUTT")
        assertTilstand(contextId2, "NY")
    }

    private fun ny(hendelse: Hendelse = HENDELSE1) = UUID.randomUUID().also { uuid ->
        CommandContext(uuid).opprett(commandContextDao, hendelse)
    }

    private fun ferdig(hendelse: Hendelse = HENDELSE1) = ny(hendelse).also { uuid ->
        commandContextDao.ferdig(hendelse, uuid)
    }

    private fun suspendert(hendelse: Hendelse = HENDELSE1) = ny(hendelse).also { uuid ->
        commandContextDao.suspendert(hendelse, uuid, listOf())
    }

    private fun feil(hendelse: Hendelse = HENDELSE1) = ny(hendelse).also { uuid ->
        commandContextDao.feil(hendelse, uuid)
    }

    private fun avbryt(contextId: UUID, vedtaksperiodeId: UUID = VEDTAKSPERIODE1) {
        commandContextDao.avbryt(vedtaksperiodeId, contextId)
    }

    private fun assertTilstand(contextId: UUID, vararg expectedTilstand: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT tilstand FROM command_context WHERE context_id = ? ORDER BY id ASC",
                    contextId
                ).map { it.string("tilstand") }.asList
            )
        }.also {
            assertEquals(expectedTilstand.toList(), it)
        }
    }


    @BeforeEach
    internal fun setup() {
        testbehov(HENDELSE1.id, "Godkjenningsbehov")
        testbehov(HENDELSE2.id, "Godkjenningsbehov")
    }
}
