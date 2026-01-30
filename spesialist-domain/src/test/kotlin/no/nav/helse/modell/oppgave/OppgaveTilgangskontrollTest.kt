package no.nav.helse.modell.oppgave

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
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
    fun `saksbehandler har ikke tilgang til oppgaveegenskap selv med alle tilgangsgrupper`(egenskap: Egenskap) {
        assertFalse(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                saksbehandlerTilgangsgrupper = Tilgangsgruppe.entries.toSet(),
                brukerroller = emptySet()
            )
        )
    }

    @ParameterizedTest(name = "egenskap={0}, grupper={1}, roller={2}")
    @MethodSource("kombinasjonerSomGirTilgang")
    fun `saksbehandler har tilgang til oppgaveegenskap`(
        egenskap: Egenskap,
        grupper: Set<Tilgangsgruppe>,
        roller: Set<Brukerrolle>
    ) {
        assertTrue(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                saksbehandlerTilgangsgrupper = grupper,
                brukerroller = roller
            )
        )
    }

    @ParameterizedTest(name = "egenskap={0}, grupper={1}, roller={2}")
    @MethodSource("kombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler har ikke tilgang til oppgaveegenskap`(
        egenskap: Egenskap,
        grupper: Set<Tilgangsgruppe>,
        roller: Set<Brukerrolle>

    ) {
        assertFalse(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                saksbehandlerTilgangsgrupper = grupper,
                brukerroller = roller
            )
        )
    }

    companion object {
        @JvmStatic
        private fun kombinasjonerSomGirTilgang(): Stream<Arguments> = Stream.of(
            Arguments.of(
                Egenskap.FORTROLIG_ADRESSE,
                setOf(Tilgangsgruppe.KODE_7),
                emptySet<Brukerrolle>()

            ),
            Arguments.of(
                Egenskap.EGEN_ANSATT,
                setOf(Tilgangsgruppe.EGEN_ANSATT),
                emptySet<Brukerrolle>()
            ),
            Arguments.of(
                Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE,
                emptySet<Tilgangsgruppe>(),
                setOf(Brukerrolle.SELVSTSTENDIG_NÆRINGSDRIVENDE_BETA)
            ),
        )

        @JvmStatic
        private fun kombinasjonerSomIkkeGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    Egenskap.FORTROLIG_ADRESSE,
                    emptySet<Tilgangsgruppe>(),
                    emptySet<Brukerrolle>()

                ),
                Arguments.of(
                    Egenskap.EGEN_ANSATT,
                    emptySet<Tilgangsgruppe>(),
                    emptySet<Brukerrolle>()

                ),
                Arguments.of(
                    Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE,
                    emptySet<Tilgangsgruppe>(),
                    emptySet<Brukerrolle>()

                ),
                Arguments.of(
                    Egenskap.FORTROLIG_ADRESSE,
                    (Tilgangsgruppe.entries - Tilgangsgruppe.KODE_7).toSet(),
                    emptySet<Brukerrolle>()

                ),
                Arguments.of(
                    Egenskap.EGEN_ANSATT,
                    (Tilgangsgruppe.entries - Tilgangsgruppe.EGEN_ANSATT).toSet(),
                    emptySet<Brukerrolle>()

                ),
            )
    }
}
