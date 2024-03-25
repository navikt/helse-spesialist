package no.nav.helse.modell.kommando

import DatabaseIntegrationTest
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Melding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CommandContextDaoTest : DatabaseIntegrationTest() {

    private val VEDTAKSPERIODE_ID1 = UUID.randomUUID()
    private val VEDTAKSPERIODE_ID2 = UUID.randomUUID()
    private lateinit var HENDELSE1: TestMelding
    private lateinit var HENDELSE2: TestMelding

    @Test
    fun `lagrer context i db`() {
        val contextId = ny()
        assertTilstand(contextId, "NY")
    }

    @Test
    fun `finner suspendert context i db`() {
        val contextId = suspendert()
        assertNotNull(commandContextDao.finnSuspendert(contextId))
        assertTilstand(contextId, "NY", "SUSPENDERT")
    }

    @Test
    fun `finner feil context i db`() {
        val contextId = feil()
        assertNotNull(commandContextDao.finnSuspendertEllerFeil(contextId))
        assertTilstand(contextId, "NY", "FEIL")
    }

    @Test
    fun `avbryter ikke seg selv`() {
        val contextId = ny()
        avbryt(contextId)
        assertTilstand(contextId, "NY")
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

    @Test
    fun `returnerer commandContextId og meldingId`() {
        val contextId1 = ny(HENDELSE1)
        val contextId2 = ny(HENDELSE2)
        val avbruttKommandokjede = avbryt(UUID.randomUUID(), HENDELSE1.vedtaksperiodeId())
        assertEquals(contextId1, avbruttKommandokjede.first().first)
        assertEquals(HENDELSE1.id, avbruttKommandokjede.first().second)
        assertTilstand(contextId2, "NY")
    }

    @Test
    fun `avbryter ikke noe når det ikke finnes eksisterende contexter`() {
        val contextId = UUID.randomUUID()
        commandContextDao.avbryt(UUID.randomUUID(), contextId)
        assertContextRad(false, contextId)
    }

    private fun ny(melding: Melding = HENDELSE1) = UUID.randomUUID().also { uuid ->
        CommandContext(uuid).opprett(commandContextDao, melding.id)
    }

    private fun ferdig(melding: Melding = HENDELSE1) = ny(melding).also { uuid ->
        commandContextDao.ferdig(melding.id, uuid)
    }

    private fun suspendert(melding: Melding = HENDELSE1) = ny(melding).also { uuid ->
        commandContextDao.suspendert(melding.id, uuid, UUID.randomUUID(), listOf())
    }

    private fun feil(melding: Melding = HENDELSE1) = ny(melding).also { uuid ->
        commandContextDao.feil(melding.id, uuid)
    }

    private fun avbryt(contextId: UUID, vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID1): List<Pair<UUID, UUID>> {
        return commandContextDao.avbryt(vedtaksperiodeId, contextId)
    }

    private fun assertTilstand(contextId: UUID, vararg expectedTilstand: String) {
        sessionOf(dataSource).use  { session ->
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

    private fun assertContextRad(finnes: Boolean, contextId: UUID) {
        val count = sessionOf(dataSource).use  {
            it.run(
                queryOf("SELECT COUNT(1) FROM command_context WHERE context_id = ?",
                contextId
                ).map { it.int(1) }.asSingle
            )!!
        }
        assertEquals(finnes, count > 0)
    }


    @BeforeEach
    internal fun setup() {
        HENDELSE1 = testhendelse(UUID.randomUUID(), vedtaksperiodeId = VEDTAKSPERIODE_ID1)
        HENDELSE2 = testhendelse(UUID.randomUUID(), vedtaksperiodeId = VEDTAKSPERIODE_ID2)
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(VEDTAKSPERIODE_ID1)
        opprettVedtaksperiode(VEDTAKSPERIODE_ID2)
        vedtakDao.opprettKobling(VEDTAKSPERIODE_ID1, HENDELSE1.id)
        vedtakDao.opprettKobling(VEDTAKSPERIODE_ID2, HENDELSE2.id)
    }
}
