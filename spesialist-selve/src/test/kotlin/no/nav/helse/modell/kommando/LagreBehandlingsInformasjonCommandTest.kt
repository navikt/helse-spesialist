package no.nav.helse.modell.kommando

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import org.junit.jupiter.api.Test

class LagreBehandlingsInformasjonCommandTest {

    private val generasjonDaoMock = mockk<GenerasjonDao>(relaxed = true)

    @Test
    fun `test at command lagrer behandlingId og tags`() {
        val selveBehandlingId = UUID.randomUUID()
        val tags = listOf("ARBEIDSGIVERUTBETALING", "PERSONUTBETALING")

        LagreBehandlingsInformasjonCommand(
            vedtaksperiodeId = UUID.randomUUID(),
            spleisBehandlingId = selveBehandlingId,
            tags = tags,
            generasjonDao = generasjonDaoMock
        ).execute(CommandContext(UUID.randomUUID()))

        verify(exactly = 1) { generasjonDaoMock.oppdaterMedBehandlingsInformasjon(any(), selveBehandlingId, tags) }
    }
}