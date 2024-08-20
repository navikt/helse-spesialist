package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.januar
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class GenerasjonTilstandTest {

    @Test
    fun `Går fra MedVedtaksforslag til VidereBehandlingAvklares dersom utbetalingen blir forkastet`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(utbetalingId)
        generasjon.assertTilstand(TilstandDto.KlarTilBehandling)
        generasjon.håndterForkastetUtbetaling(utbetalingId)
        generasjon.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        generasjon.assertUtbetaling(null)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til AvsluttetUtenVedtak ved avsluttet uten vedtak`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(UUID.randomUUID(), emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtak)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til AvsluttetUtenVedtakMedVarsler ved avsluttet uten vedtak og har varsler`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID()))
        generasjon.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(UUID.randomUUID(), emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtak)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til KlarTilBehandling når vi mottar utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        generasjon.håndterNyUtbetaling(UUID.randomUUID())
        generasjon.assertTilstand(TilstandDto.KlarTilBehandling)
    }

    @Test
    fun `Går fra KlarTilBehandling til VedtakFattet når vi mottar vedtak_fattet`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        generasjon.håndterNyUtbetaling(UUID.randomUUID())
        generasjon.assertTilstand(TilstandDto.KlarTilBehandling)
        generasjon.håndterVedtakFattet()
        generasjon.assertTilstand(TilstandDto.VedtakFattet)
    }

    @Test
    fun `Håndterer ikke vedtak fattet i VedtakFattet tilstand`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(utbetalingId)

        generasjon.assertTilstand(TilstandDto.KlarTilBehandling)
        generasjon.håndterVedtakFattet()
        generasjon.assertTilstand(TilstandDto.VedtakFattet)

        generasjon.håndterVedtakFattet()
        generasjon.assertTilstand(TilstandDto.VedtakFattet)
    }

    @Test
    fun `AUU - håndterer ikke vedtak fattet`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.assertTilstand(TilstandDto.VidereBehandlingAvklares)

        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtak)

        generasjon.håndterVedtakFattet()
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtak)
    }

    @Test
    fun `AUU - håndterer nytt varsel medfører tilstandsbytte`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)

        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtak)

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtakMedVarsler)
        generasjon.assertAntallVarsler(1)
    }

    @Test
    fun `AvsluttetUtenVedtakMedVarsler - godkjent forlengelse medfører tilstandsendring`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)

        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtak)

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtakMedVarsler)
        generasjon.assertAntallVarsler(1)

        generasjon.håndterGodkjentAvSaksbehandler()
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtak)
    }

    @Test
    fun `AvsluttetUtenVedtakMedVarsler - håndterer ikke vedtak fattet`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)

        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtak)

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtakMedVarsler)

        generasjon.håndterVedtakFattet()
        generasjon.assertTilstand(TilstandDto.AvsluttetUtenVedtakMedVarsler)
    }

    private fun Generasjon.assertTilstand(tilstandDto: TilstandDto) {
        assertEquals(tilstandDto, toDto().tilstand)
    }

    private fun Generasjon.assertAntallVarsler(antall: Int) {
        assertEquals(antall, toDto().varsler.size)
    }

    private fun Generasjon.assertUtbetaling(utbetalingId: UUID?) {
        assertEquals(utbetalingId, toDto().utbetalingId)
    }

    private fun generasjon(
        generasjonId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = 1.januar,
    ) = Generasjon(
        id = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
    )
}
