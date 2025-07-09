package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.spesialist.domain.Saksbehandler as DomainSaksbehandler

class HentSaksbehandlereQueryHandlerTest : AbstractGraphQLApiTest() {

    @Test
    fun `henter alle aktive saksbehandlere siste tre mnder`() {
        every { saksbehandlerDao.hentAlleAktiveSisteTreMnder() } returns listOf(
            DomainSaksbehandler(
                id = SaksbehandlerOid(UUID.randomUUID()),
                "Navn Navnesen",
                "navn@navnesen.no",
                "L112233"
            ),
            DomainSaksbehandler(
                id = SaksbehandlerOid(UUID.randomUUID()),
                "Test Testesen",
                "test@testesen.no",
                "L445566"
            ),
        )
        val body = runQuery("{ hentSaksbehandlere { ident } }")

        assertEquals(2, body["data"]["hentSaksbehandlere"].size())
    }
}
