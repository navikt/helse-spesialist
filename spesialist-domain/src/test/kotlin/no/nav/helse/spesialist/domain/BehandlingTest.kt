package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class BehandlingTest {

    @ParameterizedTest
    @MethodSource("utfallGittTagsSource")
    fun `tags gir utfall`(tags: Set<String>, expected: Utfall) {
        val behandling = enBehandling(tags = tags)
        assertEquals(expected, behandling.utfall())
    }

    @ParameterizedTest
    @MethodSource("exceptionGittTagsSource")
    fun `tags gir exception`(tags: Set<String>) {
        val behandling = enBehandling(tags = tags)
        assertThrows<IllegalStateException> {
            behandling.utfall()
        }
    }

    private fun enBehandling(id: UUID = UUID.randomUUID(), tags: Set<String>): Behandling = Behandling.fraLagring(
        id = SpleisBehandlingId(id),
        tags = tags,
        fødselsnummer = lagFødselsnummer(),
        fom = 1.jan(2018),
        tom = 31.jan(2018),
        skjæringstidspunkt = 1.jan(2018),
        yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
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
