package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.januar
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetUtenVedtak
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class GenerasjonTilstandTest {
    private lateinit var observer: GenerasjonTestObserver

    @BeforeEach
    internal fun beforeEach() {
        observer = GenerasjonTestObserver()
    }

    @Test
    fun `Går fra MedVedtaksforslag til VidereBehandlingAvklares dersom utbetalingen blir forkastet`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjon.håndterForkastetUtbetaling(utbetalingId)
        observer.assertUtbetaling(generasjonId, null)

        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.KlarTilBehandling, 0)
        observer.assertTilstandsendring(generasjonId, Generasjon.KlarTilBehandling, Generasjon.VidereBehandlingAvklares, 1)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til AvsluttetUtenVedtak ved avsluttet uten vedtak`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)
        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(UUID.randomUUID(), emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())

        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.AvsluttetUtenVedtak, 0)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til AvsluttetUtenVedtakMedVarsler ved avsluttet uten vedtak og har varsler`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID()), UUID.randomUUID())
        generasjon.registrer(observer)
        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(UUID.randomUUID(), emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())

        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.AvsluttetUtenVedtak, 0)
    }

    @Test
    fun `Går fra VidereBehandlingAvklares til MedVedtaksforslag når vi mottar utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.KlarTilBehandling, 0)
    }

    @Test
    fun `Går fra KlarTilBehandling til VedtakFattet når vi mottar vedtak_fattet`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.KlarTilBehandling, Generasjon.VedtakFattet, 1)
    }

    @Test
    fun `Håndterer ikke vedtak fattet i VedtakFattet tilstand`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.KlarTilBehandling, 0)
        observer.assertTilstandsendring(generasjonId, Generasjon.KlarTilBehandling, Generasjon.VedtakFattet, 1)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        observer.assertGjeldendeTilstand(generasjonId, Generasjon.VedtakFattet)
    }

    @Test
    fun `AUU - håndterer ikke vedtak fattet`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.AvsluttetUtenVedtak, 0)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        observer.assertGjeldendeTilstand(generasjonId, Generasjon.AvsluttetUtenVedtak)
    }

    @Test
    fun `AUU - håndterer nytt varsel medfører tilstandsbytte`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.AvsluttetUtenVedtak, 0)

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenVedtak, Generasjon.AvsluttetUtenVedtakMedVarsler, 1)
        assertEquals(1, observer.opprettedeVarsler.size)
    }

    @Test
    fun `AvsluttetUtenVedtakMedVarsler - godkjent forlengelse medfører tilstandsendring`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.AvsluttetUtenVedtak, 0)

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenVedtak, Generasjon.AvsluttetUtenVedtakMedVarsler, 1)
        assertEquals(1, observer.opprettedeVarsler.size)

        generasjon.håndterGodkjentAvSaksbehandler("123456", UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenVedtakMedVarsler, Generasjon.AvsluttetUtenVedtak, 2)
    }

    @Test
    fun `AvsluttetUtenVedtakMedVarsler - håndterer ikke vedtak fattet`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        observer.assertTilstandsendring(generasjonId, Generasjon.VidereBehandlingAvklares, Generasjon.AvsluttetUtenVedtak, 0)

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenVedtak, Generasjon.AvsluttetUtenVedtakMedVarsler, 1)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        observer.assertGjeldendeTilstand(generasjonId, Generasjon.AvsluttetUtenVedtakMedVarsler)
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
