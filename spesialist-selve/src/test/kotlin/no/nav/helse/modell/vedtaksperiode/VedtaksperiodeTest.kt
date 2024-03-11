package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtaksperiodeTest {

    @Disabled
    @Test
    fun `ugyldig tilstand om Spesialist mottar ny behandling når gjeldende generasjon ikke er lukket`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        assertThrows<IllegalStateException> {
            vedtaksperiode.nySpleisBehandling(nySpleisBehandling(vedtaksperiodeId))
        }
    }

    @Test
    fun `ignorerer behandling som ikke er relevant for vedtaksperioden`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val annenVedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.vedtakFattet(UUID.randomUUID())

        vedtaksperiode.nySpleisBehandling(nySpleisBehandling(annenVedtaksperiodeId))

        val antallGenerasjoner = vedtaksperiode.toDto().generasjoner.size
        assertEquals(1, antallGenerasjoner) // Det har ikke blitt opprettet noen ny generasjon for denne vedtaksperioden
    }

    @Test
    fun `ny generasjon om Spesialist mottar ny behandling når gjeldende generasjon er lukket`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.vedtakFattet(UUID.randomUUID())
        vedtaksperiode.nySpleisBehandling(nySpleisBehandling(vedtaksperiodeId))
        val nyGjeldendeGenerasjon = vedtaksperiode.toDto().generasjoner.last()
        assertEquals(TilstandDto.Ulåst, nyGjeldendeGenerasjon.tilstand)
    }

    @Test
    fun `vedtak fattet uten utbetaling medfører at generasjonen lukkes som AUU-generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.vedtakFattet(UUID.randomUUID())
        val dto = vedtaksperiode.toDto()
        val generasjon = dto.generasjoner.single()
        assertEquals(TilstandDto.AvsluttetUtenUtbetaling, generasjon.tilstand)
    }

    @Test
    fun `Kan ikke gjenopprette vedtaksperiode uten generasjoner`() {
        assertThrows<IllegalStateException> {
            Vedtaksperiode.gjenopprett("987654321", UUID.randomUUID(), emptyList())
        }
    }

    private fun nySpleisBehandling(vedtaksperiodeId: UUID) = SpleisBehandling("987654321", vedtaksperiodeId, UUID.randomUUID(), 1.januar, 31.januar)

    private fun nyVedtaksperiode(vedtaksperiodeId: UUID) = Vedtaksperiode.nyVedtaksperiode(nySpleisBehandling(vedtaksperiodeId))
}