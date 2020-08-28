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
        private val FNR = "FNR"
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val HENDELSE = TestHendelse(UUID.randomUUID(), VEDTAKSPERIODE)
    }

    private lateinit var commandContextDao: CommandContextDao

    private fun context(id: UUID) = CommandContext(id)

    @Test
    fun `lagrer og finner context i db`() {
        val contextId = UUID.randomUUID()
        context(contextId).opprett(commandContextDao, HENDELSE)
        assertNotNull(commandContextDao.finn(contextId))
        assertTilstand("NY", contextId)
    }

    @Test
    fun `avbryter ikke seg selv`() {
        val contextId = UUID.randomUUID()
        context(contextId).apply {
            opprett(commandContextDao, HENDELSE)
            avbryt(commandContextDao, VEDTAKSPERIODE)
        }
        assertTilstand("NY", contextId)
    }

    @Test
    fun `avbryter command som er NY eller SUSPENDERT`() {
        val contextId1 = UUID.randomUUID()
        val contextId2 = UUID.randomUUID()
        val contextId3 = UUID.randomUUID()
        val contextId4 = UUID.randomUUID()
        context(contextId2).opprett(commandContextDao, HENDELSE)
        context(contextId3).opprett(commandContextDao, HENDELSE)
        context(contextId4).opprett(commandContextDao, HENDELSE)
        commandContextDao.ferdig(HENDELSE, contextId4)
        commandContextDao.suspendert(HENDELSE, contextId3, listOf())

        context(contextId1).avbryt(commandContextDao, VEDTAKSPERIODE)

        assertTilstand("AVBRUTT", contextId2)
        assertTilstand("AVBRUTT", contextId3)
        assertTilstand("FERDIG", contextId4)
    }

    @Test
    fun `avbryter ikke commands som er FEIL eller FERDIG`() {
        val contextId1 = UUID.randomUUID()
        val contextId2 = UUID.randomUUID()
        val contextId3 = UUID.randomUUID()
        val contextId4 = UUID.randomUUID()
        context(contextId2).opprett(commandContextDao, HENDELSE)
        context(contextId3).opprett(commandContextDao, HENDELSE)
        context(contextId4).opprett(commandContextDao, HENDELSE)
        commandContextDao.ferdig(HENDELSE, contextId2)
        commandContextDao.feil(HENDELSE, contextId3)
        context(contextId1).avbryt(commandContextDao, VEDTAKSPERIODE)

        assertTilstand("FERDIG", contextId2)
        assertTilstand("FEIL", contextId3)
        assertTilstand("AVBRUTT", contextId4)
    }

    @Test
    fun `avbryter bare for riktig vedtaksperiode`() {
        val contextId1 = UUID.randomUUID()
        val forVedtaksperiode1 = UUID.randomUUID()
        context(forVedtaksperiode1).opprett(commandContextDao, HENDELSE)

        val hendelse2 = TestHendelse(UUID.randomUUID(), UUID.randomUUID())
        testSpleisbehov(hendelse2.id)
        val forVedtaksperiode2 = UUID.randomUUID()
        context(forVedtaksperiode2).opprett(commandContextDao, hendelse2)

        context(contextId1).avbryt(commandContextDao, VEDTAKSPERIODE)

        assertTilstand("AVBRUTT", forVedtaksperiode1)
        assertTilstand("NY", forVedtaksperiode2)
    }

    private fun assertTilstand(expectedTilstand: String, contextId: UUID) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT tilstand FROM command_context WHERE context_id = ? ORDER BY id DESC LIMIT 1",
                    contextId
                ).map { it.string("tilstand") }.asSingle
            )
        }.also {
            assertEquals(expectedTilstand, it)
        }
    }

    private fun testSpleisbehov(hendelseId: UUID = HENDELSE.id) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO spleisbehov(id, data, original, spleis_referanse, type) VALUES(?, ?::json, ?::json, ?, ?)",
                    hendelseId,
                    "{}",
                    "{}",
                    hendelseId,
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
        commandContextDao = CommandContextDao(dataSource)
    }


    @BeforeEach
    internal fun setup() {
        testSpleisbehov()
    }
}
