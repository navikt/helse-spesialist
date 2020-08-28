package no.nav.helse.modell.command.nyny

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.CommandContextDao
import org.junit.jupiter.api.BeforeAll
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
        private val HENDELSE1 = TestHendelse(UUID.randomUUID(), VEDTAKSPERIODE1)
        private val HENDELSE2 = TestHendelse(UUID.randomUUID(), VEDTAKSPERIODE2)
    }

    private lateinit var dao: CommandContextDao

    @Test
    fun `lagrer og finner context i db`() {
        val contextId = ny()
        assertNotNull(dao.finn(contextId))
        assertTilstand(contextId, "NY")
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
        CommandContext(uuid).opprett(dao, hendelse)
    }

    private fun ferdig(hendelse: Hendelse = HENDELSE1) = ny(hendelse).also { uuid ->
        dao.ferdig(hendelse, uuid)
    }

    private fun suspendert(hendelse: Hendelse = HENDELSE1) = ny(hendelse).also { uuid ->
        dao.suspendert(hendelse, uuid, listOf())
    }

    private fun feil(hendelse: Hendelse = HENDELSE1) = ny(hendelse).also { uuid ->
        dao.feil(hendelse, uuid)
    }

    private fun avbryt(contextId: UUID, vedtaksperiodeId: UUID = VEDTAKSPERIODE1) {
        dao.avbryt(vedtaksperiodeId, contextId)
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

    private fun testSpleisbehov(hendelse: Hendelse) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO spleisbehov(id, data, original, spleis_referanse, type) VALUES(?, ?::json, ?::json, ?, ?)",
                    hendelse.id,
                    "{}",
                    "{}",
                    hendelse.id,
                    "Godkjenningsbehov"
                ).asExecute
            )
        }
    }

    private class TestHendelse(override val id: UUID, private val vedtaksperiodeId: UUID) : Hendelse {
        override fun execute(context: CommandContext): Boolean {
            TODO("Not yet implemented")
        }

        override fun fødselsnummer(): String {
            return FNR
        }

        override fun vedtaksperiodeId(): UUID {
            return vedtaksperiodeId
        }

        override fun toJson(): String {
            TODO("Not yet implemented")
        }
    }

    @BeforeAll
    internal fun setupAll() {
        dao = CommandContextDao(dataSource)
    }


    @BeforeEach
    internal fun setup() {
        testSpleisbehov(HENDELSE1)
        testSpleisbehov(HENDELSE2)
    }
}
