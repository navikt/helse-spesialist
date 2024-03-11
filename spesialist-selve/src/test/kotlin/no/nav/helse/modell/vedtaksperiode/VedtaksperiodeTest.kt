package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtaksperiodeTest {

    @Test
    fun `ugyldig tilstand om Spesialist mottar ny behandling når gjeldende generasjon ikke er lukket`() {
        val vedtaksperiode = nyVedtaksperiode()
        assertThrows<IllegalStateException> {
            vedtaksperiode.nySpleisBehandling(SpleisBehandling(UUID.randomUUID()))
        }
    }

    @Test
    fun `ny generasjon om Spesialist mottar ny behandling når gjeldende generasjon er lukket`() {
        val vedtaksperiode = nyVedtaksperiode()
        vedtaksperiode.vedtakFattet(UUID.randomUUID())
        vedtaksperiode.nySpleisBehandling(SpleisBehandling(UUID.randomUUID()))
        val nyGjeldendeGenerasjon = vedtaksperiode.toDto().generasjoner.last()
        assertEquals(TilstandDto.Ulåst, nyGjeldendeGenerasjon.tilstand)
    }

    @Test
    fun `vedtak fattet uten utbetaling medfører at generasjonen lukkes som AUU-generasjon`() {
        val vedtaksperiode = nyVedtaksperiode()
        vedtaksperiode.vedtakFattet(UUID.randomUUID())
        val dto = vedtaksperiode.toDto()
        val generasjon = dto.generasjoner.single()
        assertEquals(TilstandDto.AvsluttetUtenUtbetaling, generasjon.tilstand)
    }

    @Test
    fun `Kan ikke gjenopprette vedtaksperiode uten generasjoner`() {
        assertThrows<IllegalStateException> {
            Vedtaksperiode.gjenopprett(UUID.randomUUID(), emptyList())
        }
    }

    private fun nyVedtaksperiode() = Vedtaksperiode.nyVedtaksperiode(UUID.randomUUID(), 1.januar, 31.januar, 31.januar)
}