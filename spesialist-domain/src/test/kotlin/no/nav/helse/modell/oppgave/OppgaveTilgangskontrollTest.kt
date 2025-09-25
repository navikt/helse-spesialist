package no.nav.helse.modell.oppgave

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class OppgaveTilgangskontrollTest {
    @ParameterizedTest(name = "egenskap={0}")
    @EnumSource(value = Egenskap::class, names = ["STRENGT_FORTROLIG_ADRESSE"])
    fun `saksbehandler har ikke tilgang til oppgaveegenskap selv med alle tilgangsgrupper`(egenskap: Egenskap) {
        assertFalse(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                saksbehandler = lagSaksbehandler(),
                saksbehandlerTilgangsgrupper = Tilgangsgruppe.entries.toSet()
            )
        )
    }

    @ParameterizedTest(name = "egenskap={0}, ident={1}, grupper={2}")
    @MethodSource("kombinasjonerSomGirTilgang")
    fun `saksbehandler har tilgang til oppgaveegenskap`(
        egenskap: Egenskap,
        ident: String,
        grupper: Set<Tilgangsgruppe>
    ) {
        assertTrue(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                saksbehandler = lagSaksbehandler(ident = ident),
                saksbehandlerTilgangsgrupper = grupper
            )
        )
    }

    @ParameterizedTest(name = "egenskap={0}, ident={1}, grupper={2}")
    @MethodSource("kombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler har ikke tilgang til oppgaveegenskap`(
        egenskap: Egenskap,
        ident: String,
        grupper: Set<Tilgangsgruppe>
    ) {
        assertFalse(
            Oppgave.harTilgangTilEgenskap(
                egenskap = egenskap,
                saksbehandler = lagSaksbehandler(ident = ident),
                saksbehandlerTilgangsgrupper = grupper
            )
        )
    }

    companion object {
        @JvmStatic
        private fun kombinasjonerSomGirTilgang(): Stream<Arguments> = Stream.of(
            Arguments.of(
                Egenskap.FORTROLIG_ADRESSE,
                "A123456",
                setOf(Tilgangsgruppe.KODE_7)
            ),
            Arguments.of(
                Egenskap.EGEN_ANSATT,
                "A123456",
                setOf(Tilgangsgruppe.EGEN_ANSATT)
            ),
            Arguments.of(
                Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE,
                "A123456",
                setOf(Tilgangsgruppe.TBD)
            ),
            Arguments.of(
                Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE,
                "G155258", // En coach
                emptySet<Tilgangsgruppe>()
            ),
        )

        @JvmStatic
        private fun kombinasjonerSomIkkeGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    Egenskap.FORTROLIG_ADRESSE,
                    "A123456",
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    Egenskap.EGEN_ANSATT,
                    "A123456",
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE,
                    "A123456",
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    Egenskap.FORTROLIG_ADRESSE,
                    "A123456",
                    (Tilgangsgruppe.entries - Tilgangsgruppe.KODE_7).toSet()
                ),
                Arguments.of(
                    Egenskap.EGEN_ANSATT,
                    "A123456",
                    (Tilgangsgruppe.entries - Tilgangsgruppe.EGEN_ANSATT).toSet()
                ),
                Arguments.of(
                    Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE,
                    "A123456",
                    (Tilgangsgruppe.entries - Tilgangsgruppe.TBD).toSet()
                ),
            )
    }

    private fun lagSaksbehandler(ident: String = "A123456"): Saksbehandler {
        val navn = lagFornavn() + " " + lagEtternavn()
        return Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = navn,
            epost = lagEpostadresseFraFulltNavn(navn),
            ident = ident,
        )
    }
}
