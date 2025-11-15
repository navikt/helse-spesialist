package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.domain.testfixtures.lagEnBehandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BehandlingTest {

    @ParameterizedTest()
    @MethodSource("utfallGittTagsSource")
    fun `tags gir utfall`(tags: Set<String>, expected: Utfall) {
        val behandling = lagEnBehandling(tags = tags)
        assertEquals(expected, behandling.utfall())
    }

    @ParameterizedTest
    @MethodSource("exceptionGittTagsSource")
    fun `tags gir exception`(tags: Set<String>) {
        val behandling = lagEnBehandling(tags = tags)
        assertThrows<IllegalStateException> {
            behandling.utfall()
        }
    }

    @Test
    fun `behandlingen overlapper med infotrygd`() {
        val tags = setOf("OverlapperMedInfotrygd")
        val behandling = lagEnBehandling(tags = tags)
        assertTrue(behandling.overlapperMedInfotrygd())
    }

    @Test
    fun `behandlingen overlapper ikke med infotrygd`() {
        val tags = setOf("Innvilget")
        val behandling = lagEnBehandling(tags = tags)
        assertFalse(behandling.overlapperMedInfotrygd())
    }

    private companion object {
        @JvmStatic
        fun utfallGittTagsSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(setOf("Innvilget"), Utfall.INNVILGELSE),
                Arguments.of(setOf("DelvisInnvilget"), Utfall.DELVIS_INNVILGELSE),
                Arguments.of(setOf("Avslag"), Utfall.AVSLAG),
            )
        }
        @JvmStatic
        fun exceptionGittTagsSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(setOf("Innvilget", "DelvisInnvilget")),
                Arguments.of(setOf("Innvilget", "Avslag")),
                Arguments.of(setOf("DelvisInnvilget", "Avslag")),
                Arguments.of(emptySet<String>()),
                Arguments.of(setOf("Foobar")),
            )
        }
    }
}
