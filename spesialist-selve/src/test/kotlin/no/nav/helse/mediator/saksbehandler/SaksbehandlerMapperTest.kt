package no.nav.helse.mediator.saksbehandler

import TilgangskontrollForTestHarIkkeTilgang
import java.util.UUID
import no.nav.helse.mediator.saksbehandler.SaksbehandlerMapper.tilApiversjon
import no.nav.helse.modell.saksbehandler.Saksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SaksbehandlerMapperTest {

    @Test
    fun `map modell-saksbehandler til api-saksbehandler`() {
        val oid = UUID.randomUUID()
        val saksbehandler = Saksbehandler("epost", oid, "navn", "ident", TilgangskontrollForTestHarIkkeTilgang)
        val saksbehandlerforApi = saksbehandler.tilApiversjon()
        assertEquals(oid, saksbehandlerforApi.oid)
        assertEquals("navn", saksbehandlerforApi.navn)
        assertEquals("epost", saksbehandlerforApi.epost)
        assertEquals("ident", saksbehandlerforApi.ident)
    }
}