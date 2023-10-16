package no.nav.helse.modell.sykefraværstilfelle

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.forhindrerAutomatisering
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SykefraværstilfelleTest {

    @Test
    fun `har ikke aktive varsler`() {
        val gjeldendeGenerasjon1 = generasjon(UUID.randomUUID())
        val gjeldendeGenerasjon2 = generasjon(UUID.randomUUID())
        gjeldendeGenerasjon1.håndterTidslinjeendring(1.januar, 31.januar, 1.januar, UUID.randomUUID())
        gjeldendeGenerasjon2.håndterTidslinjeendring(1.februar, 28.februar, 1.februar, UUID.randomUUID())
        assertFalse(listOf(gjeldendeGenerasjon1, gjeldendeGenerasjon2).forhindrerAutomatisering(28.februar))
    }

    @Test
    fun `har ikke aktive varsler når generasjonene har utbetalingId men ikke fom`() {
        val gjeldendeGenerasjon1 = generasjon(UUID.randomUUID())
        val gjeldendeGenerasjon2 = generasjon(UUID.randomUUID())
        val utbetalingId = UUID.randomUUID()
        gjeldendeGenerasjon1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        gjeldendeGenerasjon2.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        assertFalse(listOf(gjeldendeGenerasjon1, gjeldendeGenerasjon2).forhindrerAutomatisering(28.februar))
    }

    @Test
    fun `har ikke spesialsak som kan automatiseres hvis perioden ikke er kjent for sykefraværstilfellet`() {
        val sykefraværstilfelle = sykefraværstilfelle()
        assertFalse(sykefraværstilfelle.spesialsakSomKanAutomatiseres(UUID.randomUUID()))
    }

    @Test
    fun `har aktive varsler`() {
        val vedtaksperiodeId2 = UUID.randomUUID()
        val gjeldendeGenerasjon1 = generasjon(UUID.randomUUID())
        val gjeldendeGenerasjon2 = generasjon(vedtaksperiodeId2)
        gjeldendeGenerasjon1.håndterTidslinjeendring(1.januar, 31.januar, 1.januar, UUID.randomUUID())
        gjeldendeGenerasjon2.håndterTidslinjeendring(1.februar, 28.februar, 1.februar, UUID.randomUUID())
        gjeldendeGenerasjon2.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId2),
            UUID.randomUUID()
        )
        assertTrue(listOf(gjeldendeGenerasjon1, gjeldendeGenerasjon2).forhindrerAutomatisering(28.februar))
    }

    @Test
    fun `thrower hvis generasjon ikke finnes`() {
        assertThrows<IllegalArgumentException> { sykefraværstilfelle().haster(UUID.randomUUID()) }
    }

    private fun generasjon(vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )

    private fun sykefraværstilfelle(
        fødselsnummer: String = "12345678910",
        skjæringstidspunkt: LocalDate = 1.januar,
        gjeldendeGenerasjoner: List<Generasjon> = listOf(generasjon()),
    ) = Sykefraværstilfelle(
        fødselsnummer,
        skjæringstidspunkt,
        gjeldendeGenerasjoner,
        emptyList(),
    )
}