package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.MeldingDao
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

internal class AvbrytCommandTest : ApplicationTest() {
    private val vedtaksperiodeId = UUID.randomUUID()
    private val commandContextId = UUID.randomUUID()

    private fun lagAvbrytCommand() =
        AvbrytCommand(
            fødselsnummer = lagFødselsnummer(),
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
        )

    private val context = CommandContext(commandContextId)

    private val command = lagAvbrytCommand()

    @Test
    fun `avbryter command context`() {
        // given
        val hendelseId = UUID.randomUUID()
        val enAnnenCommandContextId = UUID.randomUUID()
        sessionContext.meldingDao.lagre(hendelseId, "{}", MeldingDao.Meldingtype.VEDTAKSPERIODE_REBEREGNET, vedtaksperiodeId)
        sessionContext.commandContextDao.opprett(hendelseId, enAnnenCommandContextId)
        // when
        val kommandoFerdig = command.execute(context, sessionContext, outbox)

        // then
        assertTrue(kommandoFerdig)
        assertEquals(mapOf(enAnnenCommandContextId to hendelseId), sessionContext.commandContextDao.avbrutteKommandokjeder)
    }
}
