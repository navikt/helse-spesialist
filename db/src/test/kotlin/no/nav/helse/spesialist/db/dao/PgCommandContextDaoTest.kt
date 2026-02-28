package no.nav.helse.spesialist.db.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.Melding
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Vedtaksperiode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class PgCommandContextDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val vedtaksperiode1 = opprettVedtaksperiode(person, arbeidsgiver)
    private val vedtaksperiode2 = opprettVedtaksperiode(person, arbeidsgiver)

    private val testhendelse1 = testhendelse(UUID.randomUUID(), vedtaksperiodeId = vedtaksperiode1.id.value, fødselsnummer = person.id.value)
    private val testhendelse2 = testhendelse(UUID.randomUUID(), vedtaksperiodeId = vedtaksperiode2.id.value, fødselsnummer = person.id.value)

    init {
        opprettBehandling(vedtaksperiode1)
        opprettBehandling(vedtaksperiode2)
        vedtakDao.opprettKobling(vedtaksperiode1.id.value, testhendelse1.id)
        vedtakDao.opprettKobling(vedtaksperiode2.id.value, testhendelse2.id)
    }

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
        avbryt(contextId, vedtaksperiode1)
        assertTilstand(contextId, "NY")
    }

    @Test
    fun `avbryter ikke commands som er FEIL eller FERDIG`() {
        val contextId1 = UUID.randomUUID()
        val contextId2 = ferdig()
        val contextId3 = feil()
        val contextId4 = ny()
        avbryt(contextId1, vedtaksperiode1)
        assertTilstand(contextId2, "NY", "FERDIG")
        assertTilstand(contextId3, "NY", "FEIL")
        assertTilstand(contextId4, "NY", "AVBRUTT")
    }

    @Test
    fun `avbryter bare for riktig vedtaksperiode`() {
        val contextId1 = ny(testhendelse1)
        val contextId2 = ny(testhendelse2)
        avbryt(UUID.randomUUID(), vedtaksperiode1)
        assertTilstand(contextId1, "NY", "AVBRUTT")
        assertTilstand(contextId2, "NY")
    }

    @Test
    fun `returnerer commandContextId og meldingId`() {
        val contextId1 = ny(testhendelse1)
        val contextId2 = ny(testhendelse2)
        val avbruttKommandokjede = avbryt(UUID.randomUUID(), vedtaksperiode1)
        assertEquals(contextId1, avbruttKommandokjede.first().first)
        assertEquals(testhendelse1.id, avbruttKommandokjede.first().second)
        assertTilstand(contextId2, "NY")
    }

    @Test
    fun `avbryter ikke noe når det ikke finnes eksisterende contexter`() {
        val contextId = UUID.randomUUID()
        commandContextDao.avbryt(UUID.randomUUID(), contextId)
        assertContextRad(false, contextId)
    }

    private fun ny(melding: Melding = testhendelse1) =
        UUID.randomUUID().also { uuid ->
            CommandContext(uuid).opprett(commandContextDao, melding.id)
        }

    private fun ferdig(melding: Melding = testhendelse1) =
        ny(melding).also { uuid ->
            commandContextDao.ferdig(melding.id, uuid)
        }

    private fun suspendert(melding: Melding = testhendelse1) =
        ny(melding).also { uuid ->
            commandContextDao.suspendert(melding.id, uuid, UUID.randomUUID(), listOf())
        }

    private fun feil(melding: Melding = testhendelse1) =
        ny(melding).also { uuid ->
            commandContextDao.feil(melding.id, uuid)
        }

    private fun avbryt(
        contextId: UUID,
        vedtaksperiode: Vedtaksperiode,
    ): List<Pair<UUID, UUID>> = commandContextDao.avbryt(vedtaksperiode.id.value, contextId)

    private fun assertTilstand(
        contextId: UUID,
        vararg expectedTilstand: String,
    ) {
        sessionOf(dataSource)
            .use { session ->
                session.run(
                    queryOf(
                        "SELECT tilstand FROM command_context WHERE context_id = ? ORDER BY id ASC",
                        contextId,
                    ).map { it.string("tilstand") }.asList,
                )
            }.also {
                assertEquals(expectedTilstand.toList(), it)
            }
    }

    private fun assertContextRad(
        @Suppress("SameParameterValue") finnes: Boolean,
        contextId: UUID,
    ) {
        val count =
            sessionOf(dataSource).use {
                it.run(
                    queryOf(
                        "SELECT COUNT(1) FROM command_context WHERE context_id = ?",
                        contextId,
                    ).map { it.int(1) }.asSingle,
                )!!
            }
        assertEquals(finnes, count > 0)
    }
}
