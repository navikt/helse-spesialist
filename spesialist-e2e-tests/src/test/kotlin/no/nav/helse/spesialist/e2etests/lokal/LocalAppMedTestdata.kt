package no.nav.helse.spesialist.e2etests.lokal

import io.ktor.server.application.Application
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.helse.spesialist.e2etests.SpesialistTestApplikasjon

private const val PORT = 8080

/**
 * Starter en lokal Spesialist med alle integrasjoner stubbet, slik E2E-testene gjør, og med et
 * endepunkt for å opprette testpersoner. Ment for å kjøre Speil lokalt mot Spesialist lokalt.
 */
fun main() {
    SpesialistTestApplikasjon(
        port = PORT,
        personPseudoIdModuleLabel = "local-app",
        ventPåKtorServer = true,
    ) { applikasjon ->
        applikasjon.apiModuleIntegrationTestFixture.addAdditionalRoutings(this)
        testpersonendepunkter(Testpersonfabrikk(applikasjon))
        skrivUtOppstartsinfo(applikasjon)
    }
}

private fun Application.testpersonendepunkter(testpersonfabrikk: Testpersonfabrikk) {
    routing {
        testpersonruter(testpersonfabrikk)
    }
}

private fun Route.testpersonruter(testpersonfabrikk: Testpersonfabrikk) {
    post("/local/testpersoner") {
        val spesifikasjon =
            if ((call.request.contentLength() ?: 0L) > 0L) {
                call.receive<Testpersonspesifikasjon>()
            } else {
                Testpersonspesifikasjon()
            }
        call.respond(testpersonfabrikk.opprettTestperson(spesifikasjon))
    }
    get("/local/testpersoner") {
        call.respond(testpersonfabrikk.opprettedeTestpersoner())
    }
}

private fun skrivUtOppstartsinfo(applikasjon: SpesialistTestApplikasjon) {
    println(
        """
        
        Spesialist kjører lokalt på http://localhost:$PORT med stubbede integrasjoner.
        
        Opprett en testperson med en oppgave til godkjenning:
            curl -X POST http://localhost:$PORT/local/testpersoner
        
        Se hvilke testpersoner som er opprettet:
            curl http://localhost:$PORT/local/testpersoner
        
        OAuth2-token:
        ${applikasjon.apiModuleIntegrationTestFixture.token}
        
        """.trimIndent(),
    )
}
