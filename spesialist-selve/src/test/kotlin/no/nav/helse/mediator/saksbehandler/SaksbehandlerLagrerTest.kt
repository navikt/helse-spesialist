package no.nav.helse.mediator.saksbehandler

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import org.junit.jupiter.api.Test

class SaksbehandlerLagrerTest {
    private val OID = UUID.randomUUID()
    private val EPOST = "saksbehandler@nav.no"
    private val NAVN = "Saksbehandler I Nav"
    private val IDENT = "S199999"
    private val dao = mockk<SaksbehandlerDao>(relaxed = true)
    private val saksbehandler = Saksbehandler(EPOST, OID, NAVN, IDENT)

    @Test
    fun `lagre saksbehandler`() {
        SaksbehandlerLagrer(dao).lagre(saksbehandler)
        verify(exactly = 1) { dao.opprettSaksbehandler(OID, NAVN, EPOST, IDENT) }
    }
 }