package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class TilgangsgrupperTilBrukerrollerTest {
    private val beslutterGruppeId = UUID.randomUUID()
    private val porteføljestyringGruppeId = UUID.randomUUID()
    private val annenGruppeId1 = UUID.randomUUID()
    private val annenGruppeId2 = UUID.randomUUID()

    private val tilgangsgrupperTilBrukerroller =
        tilgangsgrupperTilBrukerroller(
            beslutter = listOf(beslutterGruppeId),
            porteføljestyring = listOf(porteføljestyringGruppeId),
        )

    @Test
    fun `ingen grupper gir ingen roller`() {
        val roller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(emptyList())

        assertTrue(roller.isEmpty())
    }

    @Test
    fun `har beslutter-gruppe blant to grupper`() {
        val grupper = listOf(annenGruppeId1, beslutterGruppeId)
        val roller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(grupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Brukerrolle.Beslutter))
    }

    @Test
    fun `har porteføljestyring-gruppe blant flere grupper`() {
        val grupper = listOf(annenGruppeId1, porteføljestyringGruppeId)
        val roller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(grupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Brukerrolle.Porteføljestyring))
    }

    @Test
    fun `nye roller coach, fagkoordinator og supersaksbehandler mappes fra sine grupper`() {
        val coachGruppeId = UUID.randomUUID()
        val fagkoordinatorGruppeId = UUID.randomUUID()
        val supersaksbehandlerGruppeId = UUID.randomUUID()
        val tilgangsgrupperTilBrukerroller =
            tilgangsgrupperTilBrukerroller(
                coach = listOf(coachGruppeId),
                fagkoordinator = listOf(fagkoordinatorGruppeId),
                supersaksbehandler = listOf(supersaksbehandlerGruppeId),
            )

        val roller =
            tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(
                listOf(coachGruppeId, fagkoordinatorGruppeId, supersaksbehandlerGruppeId),
            )

        assertEquals(setOf(Brukerrolle.Coach, Brukerrolle.Fagkoordinator, Brukerrolle.Supersaksbehandler), roller)
    }

    @Test
    fun `tre grupper uten rollegrupper gir ingen roller`() {
        val grupper = listOf(annenGruppeId1, annenGruppeId2, UUID.randomUUID())
        val roller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(grupper)

        assertTrue(roller.isEmpty())
    }
}
