package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BehandlingTest {

    @ParameterizedTest()
    @MethodSource("utfallGittTagsSource")
    fun `tags gir utfall`(tags: Set<String>, expected: Utfall) {
        val behandling = lagEnBehandling(tags)
        assertEquals(expected, behandling.utfall())
    }

    @ParameterizedTest
    @MethodSource("exceptionGittTagsSource")
    fun `tags gir exception`(tags: Set<String>) {
        val behandling = lagEnBehandling(tags)
        assertThrows<IllegalStateException> {
            behandling.utfall()
        }
    }

    @Test
    fun `behandlingen overlapper med infotrygd`() {
        val tags = setOf("OverlapperMedInfotrygd")
        val behandling = lagEnBehandling(tags)
        assertTrue(behandling.overlapperMedInfotrygd())
    }

    @Test
    fun `behandlingen overlapper ikke med infotrygd`() {
        val tags = setOf("Innvilget")
        val behandling = lagEnBehandling(tags)
        assertFalse(behandling.overlapperMedInfotrygd())
    }

    private fun lagEnBehandling(tags: Set<String>): Behandling = Behandling.fraLagring(
        id = SpleisBehandlingId(UUID.randomUUID()),
        tags = tags,
        søknadIder = emptySet(),
        fom = 1.jan(2018),
        tom = 31.jan(2018),
        skjæringstidspunkt = 1.jan(2018),
        vedtaksperiodeId = VedtaksperiodeId(UUID.randomUUID())
    )

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
