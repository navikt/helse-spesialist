package no.nav.helse.spesialist.application.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.VedtakDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.PersisterVedtaksperiodetypeCommand
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PersisterVedtaksperiodetypeCommandTest {

    private val HENDELSE_ID = UUID.randomUUID()
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)

    @Test
    fun `Legger til vedtaksperiodetype`() {
        val type = Periodetype.FØRSTEGANGSBEHANDLING
        val inntektskilde = Inntektskilde.EN_ARBEIDSGIVER
        PersisterVedtaksperiodetypeCommand(HENDELSE_ID, type, inntektskilde, vedtakDao)
            .execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 1) { vedtakDao.leggTilVedtaksperiodetype(HENDELSE_ID, type, inntektskilde) }
    }
}
