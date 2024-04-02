package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.varsel.Varsel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    fun `vedtaksperioden mottar nye varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.nyeVarsler(listOf(nyttVarsel(vedtaksperiodeId)))
        val gjeldendeGenerasjon = vedtaksperiode.toDto().generasjoner.single()
        assertEquals(1, gjeldendeGenerasjon.varsler.size)
    }

    @Test
    fun `vedtaksperioden ignorerer varsler som ikke er relevante for den`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.nyeVarsler(listOf(nyttVarsel(UUID.randomUUID())))
        val gjeldendeGenerasjon = vedtaksperiode.toDto().generasjoner.single()
        assertEquals(0, gjeldendeGenerasjon.varsler.size)
    }

    @Test
    fun `oppretter ny generasjon om spesialist mottar ny behandling når gjeldende generasjon er låst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.nyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        vedtaksperiode.vedtakFattet(UUID.randomUUID())
        vedtaksperiode.nySpleisBehandling(nySpleisBehandling(vedtaksperiodeId))
        val generasjoner = vedtaksperiode.toDto().generasjoner
        val nyGjeldendeGenerasjon = generasjoner.last()
        assertEquals(TilstandDto.Ulåst, nyGjeldendeGenerasjon.tilstand)
        assertEquals(2, generasjoner.size)
    }
    @Test
    fun `oppretter ny generasjon om spesialist mottar ny behandling når gjeldende generasjon er AUU`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        vedtaksperiode.vedtakFattet(UUID.randomUUID())
        vedtaksperiode.nySpleisBehandling(nySpleisBehandling(vedtaksperiodeId))
        val generasjoner = vedtaksperiode.toDto().generasjoner
        val nyGjeldendeGenerasjon = generasjoner.last()
        assertEquals(TilstandDto.Ulåst, nyGjeldendeGenerasjon.tilstand)
        assertEquals(2, generasjoner.size)
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
    fun `ny vedtaksperiode opprettes med spleisBehandlingId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiode = nyVedtaksperiode(vedtaksperiodeId)
        val dto = vedtaksperiode.toDto()
        assertNotNull(dto.generasjoner.single().spleisBehandlingId)
    }

    @Test
    fun `Kan ikke gjenopprette vedtaksperiode uten generasjoner`() {
        assertThrows<IllegalStateException> {
            Vedtaksperiode.gjenopprett("987654321", UUID.randomUUID(), false, emptyList())
        }
    }

    private fun nySpleisBehandling(vedtaksperiodeId: UUID) = SpleisBehandling("987654321", vedtaksperiodeId, UUID.randomUUID(), 1.januar, 31.januar)

    private fun nyttVarsel(vedtaksperiodeId: UUID, varselkode: String = "SB_EX_1") = Varsel(UUID.randomUUID(), varselkode, LocalDateTime.now(), vedtaksperiodeId)

    private fun nyVedtaksperiode(vedtaksperiodeId: UUID) = Vedtaksperiode.nyVedtaksperiode(nySpleisBehandling(vedtaksperiodeId))
}