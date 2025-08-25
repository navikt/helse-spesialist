package no.nav.helse.modell.person.vedtaksperiode

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class LegacyBehandlingTilstandTest {

    @Test
    fun `Går fra MedVedtaksforslag til VidereBehandlingAvklares dersom utbetalingen blir forkastet`() {
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(behandlingId, UUID.randomUUID())

        val utbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(utbetalingId)
        behandling.assertTilstand(TilstandDto.KlarTilBehandling)
        behandling.håndterForkastetUtbetaling(utbetalingId)
        behandling.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        behandling.assertUtbetaling(null)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til AvsluttetUtenVedtak ved avsluttet uten vedtak`() {
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(behandlingId, UUID.randomUUID())
        behandling.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        behandling.avsluttetUtenVedtak()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtak)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til AvsluttetUtenVedtakMedVarsler ved avsluttet uten vedtak og har varsler`() {
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(behandlingId, UUID.randomUUID())
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID()))
        behandling.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        behandling.avsluttetUtenVedtak()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtak)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til KlarTilBehandling når vi mottar utbetaling`() {
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(behandlingId, UUID.randomUUID())
        behandling.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        behandling.håndterNyUtbetaling(UUID.randomUUID())
        behandling.assertTilstand(TilstandDto.KlarTilBehandling)
    }

    @Test
    fun `Går fra KlarTilBehandling til VedtakFattet når vi mottar vedtak_fattet`() {
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(behandlingId, UUID.randomUUID())
        behandling.assertTilstand(TilstandDto.VidereBehandlingAvklares)
        behandling.håndterNyUtbetaling(UUID.randomUUID())
        behandling.assertTilstand(TilstandDto.KlarTilBehandling)
        behandling.håndterVedtakFattet()
        behandling.assertTilstand(TilstandDto.VedtakFattet)
    }

    @Test
    fun `Håndterer ikke vedtak fattet i VedtakFattet tilstand`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(behandlingId, vedtaksperiodeId)

        val utbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(utbetalingId)

        behandling.assertTilstand(TilstandDto.KlarTilBehandling)
        behandling.håndterVedtakFattet()
        behandling.assertTilstand(TilstandDto.VedtakFattet)

        behandling.håndterVedtakFattet()
        behandling.assertTilstand(TilstandDto.VedtakFattet)
    }

    @Test
    fun `AUU - håndterer ikke vedtak fattet`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(behandlingId, vedtaksperiodeId)
        behandling.assertTilstand(TilstandDto.VidereBehandlingAvklares)

        behandling.avsluttetUtenVedtak()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtak)

        behandling.håndterVedtakFattet()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtak)
    }

    @Test
    fun `AUU - håndterer nytt varsel medfører tilstandsbytte`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(behandlingId, vedtaksperiodeId)

        behandling.avsluttetUtenVedtak()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtak)

        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtakMedVarsler)
        behandling.assertAntallVarsler(1)
    }

    @Test
    fun `AvsluttetUtenVedtakMedVarsler - godkjent forlengelse medfører tilstandsendring`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(behandlingId, vedtaksperiodeId)

        behandling.avsluttetUtenVedtak()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtak)

        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtakMedVarsler)
        behandling.assertAntallVarsler(1)

        behandling.håndterGodkjentAvSaksbehandler()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtak)
    }

    @Test
    fun `AvsluttetUtenVedtakMedVarsler - håndterer ikke vedtak fattet`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(behandlingId, vedtaksperiodeId)

        behandling.avsluttetUtenVedtak()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtak)

        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtakMedVarsler)

        behandling.håndterVedtakFattet()
        behandling.assertTilstand(TilstandDto.AvsluttetUtenVedtakMedVarsler)
    }

    private fun LegacyBehandling.assertTilstand(tilstandDto: TilstandDto) {
        assertEquals(tilstandDto, toDto().tilstand)
    }

    private fun LegacyBehandling.assertAntallVarsler(antall: Int) {
        assertEquals(antall, toDto().varsler.size)
    }

    private fun LegacyBehandling.assertUtbetaling(utbetalingId: UUID?) {
        assertEquals(utbetalingId, toDto().utbetalingId)
    }

    private fun behandling(
        behandlingId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1 jan 2018,
        tom: LocalDate = 31 jan 2018,
        skjæringstidspunkt: LocalDate = 1 jan 2018,
    ) = LegacyBehandling(
        id = behandlingId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
    )
}
