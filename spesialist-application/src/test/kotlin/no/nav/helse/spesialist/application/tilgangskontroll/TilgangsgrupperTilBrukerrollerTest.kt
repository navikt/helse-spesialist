package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class TilgangsgrupperTilBrukerrollerTest {

    private val næringsdrivendeGruppeId = UUID.randomUUID()
    private val annenGruppeId1 = UUID.randomUUID()
    private val annenGruppeId2 = UUID.randomUUID()

    private val tilgangsgrupperTilBrukerroller = TilgangsgrupperTilBrukerroller(
        næringsdrivendeBeta = listOf(næringsdrivendeGruppeId),
        beslutter = emptyList(),
        egenAnsatt = emptyList(),
        kode7 = emptyList(),
        stikkprøve = emptyList()
    )

    @Test
    fun `ingen grupper gir ingen roller`() {
        val roller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(emptyList())

        assertTrue(roller.isEmpty())
    }

    @Test
    fun `har næringsdrivende beta-gruppe blant to grupper`() {
        val grupper = listOf(annenGruppeId1, næringsdrivendeGruppeId)
        val roller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(grupper)

        assertEquals(1, roller.size)
        assertTrue(roller.contains(Brukerrolle.SELVSTSTENDIG_NÆRINGSDRIVENDE_BETA))
    }

    @Test
    fun `tre grupper uten næringsdrivende beta-gruppe gir ingen roller`() {
        val grupper = listOf(annenGruppeId1, annenGruppeId2, UUID.randomUUID())
        val roller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(grupper)

        assertTrue(roller.isEmpty())
    }
}
