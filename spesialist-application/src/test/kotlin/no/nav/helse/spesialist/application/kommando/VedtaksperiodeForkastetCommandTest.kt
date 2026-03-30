package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.MeldingDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastetCommand
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class VedtaksperiodeForkastetCommandTest : ApplicationTest() {
    private val hendelseId = UUID.randomUUID()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val contextId = UUID.randomUUID()
    private val fødselsnummer = lagFødselsnummer()

    private val context = CommandContext(contextId)
    private val vedtaksperiodeForkastetCommand =
        VedtaksperiodeForkastetCommand(
            fødselsnummer = fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
            id = hendelseId,
            alleForkastedeVedtaksperiodeIder = emptyList(),
        )

    @Test
    fun `avbryter kommandoer og markerer vedtaksperiode som forkastet`() {
        // given
        val hendelseId = UUID.randomUUID()
        val enAnnenCommandContextId = UUID.randomUUID()
        sessionContext.meldingDao.lagre(hendelseId, "{}", MeldingDao.Meldingtype.VEDTAKSPERIODE_REBEREGNET, vedtaksperiodeId)
        sessionContext.commandContextDao.opprett(hendelseId, enAnnenCommandContextId)

        // when
        val kommandoFerdig = vedtaksperiodeForkastetCommand.execute(context, sessionContext, outbox)

        // then
        assertTrue(kommandoFerdig)
        assertEquals(mapOf(enAnnenCommandContextId to hendelseId), sessionContext.commandContextDao.avbrutteKommandokjeder)
    }
}
