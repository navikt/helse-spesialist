package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BehandlingTest {
    @ParameterizedTest
    @MethodSource("utfallGittTagsSource")
    fun `tags gir utfall`(
        tags: Set<String>,
        expected: Utfall,
    ) {
        val behandling = lagBehandling(tags = tags)
        assertEquals(expected, behandling.utfall())
    }

    @ParameterizedTest
    @MethodSource("exceptionGittTagsSource")
    fun `tags gir exception`(tags: Set<String>) {
        val behandling = lagBehandling(tags = tags)
        assertThrows<IllegalStateException> {
            behandling.utfall()
        }
    }

    @Test
    fun `behandlingen overlapper med infotrygd`() {
        val tags = setOf("OverlapperMedInfotrygd")
        val behandling = lagBehandling(tags = tags)
        assertTrue(behandling.overlapperMedInfotrygd())
    }

    @Test
    fun `behandlingen overlapper ikke med infotrygd`() {
        val tags = setOf("Innvilget")
        val behandling = lagBehandling(tags = tags)
        assertFalse(behandling.overlapperMedInfotrygd())
    }

    @Test
    fun `overlapperMed er true når en periode overlapper helt`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
        val perioder = listOf(Periode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31)))
        assertTrue(behandling.overlapperMed(perioder))
    }

    @Test
    fun `overlapperMed er true når en periode overlapper delvis i starten`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 10), tom = LocalDate.of(2023, 1, 31))
        val perioder = listOf(Periode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10)))
        assertTrue(behandling.overlapperMed(perioder))
    }

    @Test
    fun `overlapperMed er true når en periode overlapper delvis på slutten`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10))
        val perioder = listOf(Periode(fom = LocalDate.of(2023, 1, 10), tom = LocalDate.of(2023, 1, 31)))
        assertTrue(behandling.overlapperMed(perioder))
    }

    @Test
    fun `overlapperMed er true når behandlingens periode ligger innenfor en av periodene`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 10), tom = LocalDate.of(2023, 1, 20))
        val perioder = listOf(Periode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31)))
        assertTrue(behandling.overlapperMed(perioder))
    }

    @Test
    fun `overlapperMed er true når en av flere perioder overlapper`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
        val perioder =
            listOf(
                Periode(fom = LocalDate.of(2022, 1, 1), tom = LocalDate.of(2022, 1, 31)),
                Periode(fom = LocalDate.of(2023, 1, 20), tom = LocalDate.of(2023, 2, 10)),
            )
        assertTrue(behandling.overlapperMed(perioder))
    }

    @Test
    fun `overlapperMed er false når ingen perioder overlapper`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
        val perioder = listOf(Periode(fom = LocalDate.of(2023, 2, 1), tom = LocalDate.of(2023, 2, 28)))
        assertFalse(behandling.overlapperMed(perioder))
    }

    @Test
    fun `overlapperMed er false når perioden ligger rett før behandlingens periode`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 11), tom = LocalDate.of(2023, 1, 31))
        val perioder = listOf(Periode(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10)))
        assertFalse(behandling.overlapperMed(perioder))
    }

    @Test
    fun `overlapperMed er false når perioden ligger rett etter behandlingens periode`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 10))
        val perioder = listOf(Periode(fom = LocalDate.of(2023, 1, 11), tom = LocalDate.of(2023, 1, 31)))
        assertFalse(behandling.overlapperMed(perioder))
    }

    @Test
    fun `overlapperMed er false når det ikke finnes noen perioder`() {
        val behandling = lagBehandling(fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
        assertFalse(behandling.overlapperMed(emptyList()))
    }

    private companion object {
        @JvmStatic
        fun utfallGittTagsSource(): Stream<Arguments> =
            Stream.of(
                Arguments.of(setOf("Innvilget"), Utfall.INNVILGELSE),
                Arguments.of(setOf("DelvisInnvilget"), Utfall.DELVIS_INNVILGELSE),
                Arguments.of(setOf("Avslag"), Utfall.AVSLAG),
            )

        @JvmStatic
        fun exceptionGittTagsSource(): Stream<Arguments> =
            Stream.of(
                Arguments.of(setOf("Innvilget", "DelvisInnvilget")),
                Arguments.of(setOf("Innvilget", "Avslag")),
                Arguments.of(setOf("DelvisInnvilget", "Avslag")),
                Arguments.of(emptySet<String>()),
                Arguments.of(setOf("Foobar")),
            )
    }
}
