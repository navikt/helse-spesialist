package no.nav.helse.spesialist.application

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.mediator.saksbehandler.SaksbehandlerLagrer
import no.nav.helse.modell.saksbehandler.Saksbehandler
import org.junit.jupiter.api.Test
import java.util.UUID

class SaksbehandlerLagrerTest {
    private val OID = UUID.randomUUID()
    private val EPOST = "saksbehandler@nav.no"
    private val NAVN = "Saksbehandler I Nav"
    private val IDENT = "S199999"
    private val dao = mockk<SaksbehandlerDao>(relaxed = true)
    private val saksbehandler = Saksbehandler(EPOST, OID, NAVN, IDENT) { _, _ -> false }

    @Test
    fun `lagre saksbehandler`() {
        SaksbehandlerLagrer(dao).lagre(saksbehandler)
        verify(exactly = 1) { dao.opprettEllerOppdater(OID, NAVN, EPOST, IDENT) }
        verify(exactly = 1) { dao.oppdaterSistObservert(OID, any()) }
    }
 }
