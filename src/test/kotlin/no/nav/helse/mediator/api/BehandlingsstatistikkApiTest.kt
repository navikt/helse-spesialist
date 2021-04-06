package no.nav.helse.mediator.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.BehandlingsstatistikkMediator
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingsstatistikkDto
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingstatistikkForSpeilDto
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingstatistikkForSpeilDto.Companion.toSpeilMap
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BehandlingsstatistikkApiTest {
    private val behandlingsstatistikkMediator = mockk<BehandlingsstatistikkMediator> {
        every { hentSaksbehandlingsstatistikk() } returns toSpeilMap(
            BehandlingsstatistikkDto(
                BehandlingsstatistikkDto.OppgaverTilGodkjenningDto(1, mapOf(FØRSTEGANGSBEHANDLING to 1)),
                0,
                0,
                0
            ))
    }

    @Test
    fun `hente ut behandlingsstatistikk`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            routing {
                behandlingsstatistikkApi(behandlingsstatistikkMediator)
            }
        }) {
            with(handleRequest(HttpMethod.Get, "/api/behandlingsstatistikk")) {
                val deserialized = objectMapper.readValue<BehandlingstatistikkForSpeilDto>(response.content!!)
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(0, deserialized.antallAnnulleringer)
                assertEquals(0, deserialized.antallGodkjenteOppgaver)
                assertEquals(0, deserialized.antallTildelteOppgaver)
                assertEquals(1, deserialized.antallOppgaverTilGodkjenning.totalt)
                assertEquals(1, deserialized.antallOppgaverTilGodkjenning.perPeriodetype[BehandlingstatistikkForSpeilDto.PeriodetypeForSpeil.FØRSTEGANGSBEHANDLING])
            }
        }
    }
}
