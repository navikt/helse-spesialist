package no.nav.helse.modell.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.VedtakRepository
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PersisterVedtaksperiodetypeCommandTest {

    private val HENDELSE_ID = UUID.randomUUID()
    private val vedtakRepository = mockk<VedtakRepository>(relaxed = true)

    @Test
    fun `Legger til vedtaksperiodetype`() {
        val type = Periodetype.FØRSTEGANGSBEHANDLING
        val inntektskilde = Inntektskilde.EN_ARBEIDSGIVER
        PersisterVedtaksperiodetypeCommand(HENDELSE_ID, type, inntektskilde, vedtakRepository)
            .execute(CommandContext(UUID.randomUUID()))
        verify(exactly = 1) { vedtakRepository.leggTilVedtaksperiodetype(HENDELSE_ID, type, inntektskilde) }
    }
}
