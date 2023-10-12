package no.nav.helse.e2e

import AbstractE2ETest
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.mediator.api.AbstractApiTest
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.spesialist.api.abonnement.AbonnementDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDto
import no.nav.helse.spesialist.api.abonnement.OpptegnelseMediator
import no.nav.helse.spesialist.api.abonnement.opptegnelseApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class OpptegnelseE2ETest : AbstractE2ETest() {
    private val SAKSBEHANDLER_ID = UUID.randomUUID()

    private val saksbehandlerDao = SaksbehandlerDao(dataSource)

    @Test
    fun `Ved abonnering får du et nytt abonnement`() {
        setupSaksbehandler()
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeNyUtbetaling()

        val opptegnelseMediator = OpptegnelseMediator(OpptegnelseDao(dataSource), AbonnementDao(dataSource))

        val respons =
            AbstractApiTest.TestServer { opptegnelseApi(opptegnelseMediator) }
                .withAuthenticatedServer {
                    it.post("/api/opptegnelse/abonner/$AKTØR") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        authentication(SAKSBEHANDLER_ID)
                    }
                }

        assertEquals(HttpStatusCode.OK, respons.status)
        håndterUtbetalingFeilet()

        val opptegnelser =
            AbstractApiTest.TestServer { opptegnelseApi(opptegnelseMediator) }
                .withAuthenticatedServer {
                    it.get("/api/opptegnelse/hent") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        authentication(SAKSBEHANDLER_ID)
                    }.body<List<OpptegnelseDto>>()
                }

        assertEquals(1, opptegnelser.size)

        val oppdateringer =
            AbstractApiTest.TestServer { opptegnelseApi(opptegnelseMediator) }
                .withAuthenticatedServer {
                    it.get("/api/opptegnelse/hent/${opptegnelser[0].sekvensnummer}") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        authentication(SAKSBEHANDLER_ID)
                    }.body<List<OpptegnelseDto>>()
                }

        assertEquals(0, oppdateringer.size)
    }

    private fun setupSaksbehandler() {
        saksbehandlerDao.opprettSaksbehandler(
            SAKSBEHANDLER_ID,
            "Saksbehandler Saksbehandlersen",
            "saksbehandler@nav.no",
            "Z999999"
        )
    }
}
