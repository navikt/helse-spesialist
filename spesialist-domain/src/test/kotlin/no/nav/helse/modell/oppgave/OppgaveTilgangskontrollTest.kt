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
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream

class OppgaveTilgangskontrollTest {
    @ParameterizedTest(name = "egenskaper={0}")
    @MethodSource("oppgaveKombinasjonerIngenHarTilgangTil")
    fun `saksbehandler har ikke tilgang til oppgave selv med alle tilganger`(egenskaper: Set<Egenskap>) {
        assertFalse(
            harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                saksbehandlerTilgangsgrupper = Tilgangsgruppe.entries.toSet()
            )
        )
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomGirTilgang")
    fun `saksbehandler med visse grupper har tilgang til visse oppgaver`(
        egenskaper: Set<Egenskap>,
        grupper: Set<Tilgangsgruppe>
    ) {
        assertTrue(
            harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                saksbehandlerTilgangsgrupper = grupper
            )
        )
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler med visse grupper har ikke tilgang til visse oppgaver`(
        egenskaper: Set<Egenskap>,
        grupper: Set<Tilgangsgruppe>
    ) {
        assertFalse(
            harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                saksbehandlerTilgangsgrupper = grupper
            )
        )
    }

    companion object {
        @JvmStatic
        private fun oppgaveKombinasjonerIngenHarTilgangTil(): Stream<Arguments> = Stream.of(
            Arguments.of(
                setOf(Egenskap.STRENGT_FORTROLIG_ADRESSE)
            )
        )

        @JvmStatic
        private fun oppgaveKombinasjonerSomGirTilgang(): Stream<Arguments> = Stream.of(
            Arguments.of(
                emptySet<Egenskap>(),
                emptySet<Tilgangsgruppe>()
            ),
            Arguments.of(
                setOf(Egenskap.FORTROLIG_ADRESSE),
                setOf(Tilgangsgruppe.KODE7)
            ),
            Arguments.of(
                setOf(Egenskap.EGEN_ANSATT),
                setOf(Tilgangsgruppe.SKJERMEDE)
            ),
            Arguments.of(
                setOf(Egenskap.BESLUTTER),
                setOf(Tilgangsgruppe.BESLUTTER)
            ),
            Arguments.of(
                setOf(Egenskap.STIKKPRØVE),
                setOf(Tilgangsgruppe.STIKKPRØVE)
            ),
        )

        @JvmStatic
        private fun oppgaveKombinasjonerSomIkkeGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    setOf(Egenskap.FORTROLIG_ADRESSE),
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.EGEN_ANSATT),
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.BESLUTTER),
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.STIKKPRØVE),
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.FORTROLIG_ADRESSE),
                    (Tilgangsgruppe.entries - Tilgangsgruppe.KODE7).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.EGEN_ANSATT),
                    (Tilgangsgruppe.entries - Tilgangsgruppe.SKJERMEDE).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.BESLUTTER),
                    (Tilgangsgruppe.entries - Tilgangsgruppe.BESLUTTER).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.STIKKPRØVE),
                    (Tilgangsgruppe.entries - Tilgangsgruppe.STIKKPRØVE).toSet()
                ),
            )
    }

    private fun harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
        egenskaper: Set<Egenskap>,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        saksbehandlerTilgangsgrupper: Set<Tilgangsgruppe>
    ): Boolean =
        egenskaper.all {
            Oppgave.harTilgangTilEgenskap(
                egenskap = it,
                saksbehandler = saksbehandler,
                saksbehandlerTilgangsgrupper = saksbehandlerTilgangsgrupper
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
