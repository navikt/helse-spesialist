package no.nav.helse.modell.command.nyny

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.junit.jupiter.api.Test
import java.util.*

internal class PersisterVedtaksperiodetypeCommandTest {

    private val HENDELSE_ID = UUID.randomUUID()
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)

    @Test
    fun `Legger til vedtaksperiodetype dersom denne er satt på hendelse`() {
        PersisterVedtaksperiodetypeCommand(HENDELSE_ID, Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING, vedtakDao)
            .execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 1) { vedtakDao.leggTilVedtaksperiodetype(HENDELSE_ID, any()) }
    }

    @Test
    fun `Legger ikke til vedtaksperiodetype når denne ikke er satt på hendelse`() {
        PersisterVedtaksperiodetypeCommand(HENDELSE_ID, null, vedtakDao)
            .execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 0) { vedtakDao.leggTilVedtaksperiodetype(HENDELSE_ID, any()) }
    }
}
