package no.nav.helse.modell.oppgave

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class OppgaveTilgangskontrollTest {
    @ParameterizedTest(name = "egenskap={0}")
    @EnumSource(value = Egenskap::class, names = ["STRENGT_FORTROLIG_ADRESSE"])
    fun `saksbehandler har ikke tilgang til oppgaveegenskap selv med alle brukerroller`(egenskap: Egenskap) {
        assertFalse(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                brukerroller = emptySet(),
            ),
        )
    }

    @ParameterizedTest(name = "egenskap={0}, roller={1}")
    @MethodSource("kombinasjonerSomGirTilgang")
    fun `saksbehandler har tilgang til oppgaveegenskap`(
        egenskap: Egenskap,
        roller: Set<Brukerrolle>,
    ) {
        assertTrue(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                brukerroller = roller,
            ),
        )
    }

    @ParameterizedTest(name = "egenskap={0}, roller={1}")
    @MethodSource("kombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler har ikke tilgang til oppgaveegenskap`(
        egenskap: Egenskap,
        roller: Set<Brukerrolle>,
    ) {
        assertFalse(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                brukerroller = roller,
            ),
        )
    }

    companion object {
        @JvmStatic
        private fun kombinasjonerSomGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    Egenskap.FORTROLIG_ADRESSE,
                    setOf(Brukerrolle.KODE_7),
                ),
                Arguments.of(
                    Egenskap.EGEN_ANSATT,
                    setOf(Brukerrolle.EGEN_ANSATT),
                ),
                Arguments.of(
                    Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE,
                    setOf(Brukerrolle.SELVSTENDIG_NÆRINGSDRIVENDE_BETA),
                ),
            )

        @JvmStatic
        private fun kombinasjonerSomIkkeGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    Egenskap.FORTROLIG_ADRESSE,
                    emptySet<Brukerrolle>(),
                ),
                Arguments.of(
                    Egenskap.EGEN_ANSATT,
                    emptySet<Brukerrolle>(),
                ),
                Arguments.of(
                    Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE,
                    emptySet<Brukerrolle>(),
                ),
                Arguments.of(
                    Egenskap.FORTROLIG_ADRESSE,
                    (Brukerrolle.entries - Brukerrolle.KODE_7).toSet(),
                ),
                Arguments.of(
                    Egenskap.EGEN_ANSATT,
                    (Brukerrolle.entries - Brukerrolle.EGEN_ANSATT).toSet(),
                ),
            )
    }
}
