package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.varsel.Varsel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GenerasjonTilstandTest {
    private lateinit var observer: GenerasjonTestObserver

    @BeforeEach
    internal fun beforeEach() {
        observer = GenerasjonTestObserver()
    }

    @Test
    fun `Ulåst - håndterNyGenerasjon lager ikke ny generasjon`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)

        // Oppretter ikke ny generasjon i ulåst
        generasjon.håndterNyGenerasjon(UUID.randomUUID())
        assertEquals(0, observer.opprettedeGenerasjoner.size)
    }
    @Test
    fun `Ulåst - håndterTidslinjeendring`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)

        generasjon.håndterTidslinjeendring(1.februar, 28.februar, 1.februar, UUID.randomUUID())
        observer.assertTidslinjeendring(generasjonId, 1.februar, 28.februar, 1.februar)
    }

    @Test
    fun `Ulåst - håndterNyUtbetaling`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        observer.assertUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `Ulåst - invaliderUtbetaling`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjon.invaliderUtbetaling(utbetalingId)
        observer.assertUtbetaling(generasjonId, null)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)
    }

    @Test
    fun `Ulåst - håndterVedtakFattet`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)
    }

    @Test
    fun `Låst - håndterer ikke vedtak fattet`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.Låst, 0)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
    }

    @Test
    fun `Låst - håndterer ny generasjon`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.Låst, 0)

        generasjon.håndterNyGenerasjon(UUID.randomUUID())
        assertEquals(1, observer.opprettedeGenerasjoner.size)
    }

    @Test
    fun `Låst - håndterer tidslinjeendring`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.Låst, 0)

        generasjon.håndterTidslinjeendring(1.februar, 28.februar, 1.februar, hendelseId)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
        observer.assertOpprettelse(vedtaksperiodeId, hendelseId, 1.februar, 28.februar, 1.februar)
    }

    @Test
    fun `Låst - håndterer ny utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        val utbetalingId1 = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId1)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.Låst, 0)

        val utbetalingId2 = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId2)
        assertEquals(2, observer.utbetalingerPåGenerasjoner.size)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
    }

    @Test
    fun `Låst - invalidering av utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        val utbetalingId1 = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId1)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.Låst, 0)

        generasjon.invaliderUtbetaling(utbetalingId1)
        observer.assertUtbetaling(generasjonId, utbetalingId1)
    }

    @Test
    fun `AUU - håndterer ny generasjon`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndterNyGenerasjon(UUID.randomUUID())
        assertEquals(1, observer.opprettedeGenerasjoner.size)
    }

    @Test
    fun `AUU - håndterer tidslinjeendring`() {
        val generasjonId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndterTidslinjeendring(1.februar, 28.februar, 1.februar, hendelseId)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
        observer.assertOpprettelse(vedtaksperiodeId, hendelseId, 1.februar, 28.februar, 1.februar)
    }

    @Test
    fun `AUU - håndterer ikke vedtak fattet`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
    }

    @Test
    fun `AUU - håndterer ny utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        val utbetalingId1 = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId1)
        assertEquals(1, observer.utbetalingerPåGenerasjoner.size)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
    }

    @Test
    fun `AUU - invaliderer ikke utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        assertEquals(1, observer.utbetalingerPåGenerasjoner.size)
        generasjon.invaliderUtbetaling(utbetalingId)
        assertEquals(1, observer.utbetalingerPåGenerasjoner.size)
        observer.assertUtbetaling(generasjonId, null)
    }

    @Test
    fun `AUU - håndterer nytt varsel medfører tilstandsbytte`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenUtbetaling, Generasjon.UtenUtbetalingMåVurderes, 1)
        assertEquals(1, observer.opprettedeVarsler.size)
    }

    @Test
    fun `UtenUtbetalingMåVurderes - godkjent forlengelse medfører tilstandsendring`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenUtbetaling, Generasjon.UtenUtbetalingMåVurderes, 1)
        assertEquals(1, observer.opprettedeVarsler.size)

        generasjon.håndterGodkjentAvSaksbehandler("123456")
        observer.assertTilstandsendring(generasjonId, Generasjon.UtenUtbetalingMåVurderes, Generasjon.AvsluttetUtenUtbetaling, 2)
    }

    @Test
    fun `UtenUtbetalingMåVurderes - håndterer ny generasjon`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenUtbetaling, Generasjon.UtenUtbetalingMåVurderes, 1)

        generasjon.håndterNyGenerasjon(UUID.randomUUID())
        assertEquals(1, observer.opprettedeGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.UtenUtbetalingMåVurderes, Generasjon.AvsluttetUtenUtbetaling, 2)

    }

    @Test
    fun `UtenUtbetalingMåVurderes - håndterer tidslinjeendring`() {
        val generasjonId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenUtbetaling, Generasjon.UtenUtbetalingMåVurderes, 1)

        generasjon.håndterTidslinjeendring(1.februar, 28.februar, 1.februar, hendelseId)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
        observer.assertOpprettelse(vedtaksperiodeId, hendelseId, 1.februar, 28.februar, 1.februar)
        observer.assertTilstandsendring(generasjonId, Generasjon.UtenUtbetalingMåVurderes, Generasjon.AvsluttetUtenUtbetaling, 2)
    }

    @Test
    fun `UtenUtbetalingMåVurderes - håndterer ikke vedtak fattet`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenUtbetaling, Generasjon.UtenUtbetalingMåVurderes, 1)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        observer.assertGjeldendeTilstand(generasjonId, Generasjon.UtenUtbetalingMåVurderes)
    }

    @Test
    fun `UtenUtbetalingMåVurderes - håndterer ny utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenUtbetaling, Generasjon.UtenUtbetalingMåVurderes, 1)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        observer.assertTilstandsendring(generasjonId, Generasjon.UtenUtbetalingMåVurderes, Generasjon.AvsluttetUtenUtbetaling, 2)
        assertEquals(1, observer.utbetalingerPåGenerasjoner.size)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
    }

    @Test
    fun `UtenUtbetalingMåVurderes - invaliderer ikke utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)

        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)

        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        observer.assertTilstandsendring(generasjonId, Generasjon.AvsluttetUtenUtbetaling, Generasjon.UtenUtbetalingMåVurderes, 1)

        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        observer.assertGjeldendeTilstand(generasjonId, Generasjon.AvsluttetUtenUtbetaling)
        assertEquals(1, observer.utbetalingerPåGenerasjoner.size)
        generasjon.invaliderUtbetaling(utbetalingId)
        assertEquals(1, observer.utbetalingerPåGenerasjoner.size)
        observer.assertUtbetaling(generasjonId, null)
        observer.assertGjeldendeTilstand(generasjonId, Generasjon.AvsluttetUtenUtbetaling)
    }


    @Test
    fun `endrer tilstand etter vedtak fattet - med utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.Låst, 0)
    }

    @Test
    fun `endrer tilstand etter vedtak fattet - uten utbetaling`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.AvsluttetUtenUtbetaling, 0)
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
        skjæringstidspunkt = skjæringstidspunkt
    )
}