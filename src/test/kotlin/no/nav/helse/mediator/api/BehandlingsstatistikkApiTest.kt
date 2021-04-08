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
import no.nav.helse.modell.oppgave.behandlingsstatistikk.BehandlingstatistikkForSpeilDto.PeriodetypeForSpeil
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BehandlingsstatistikkApiTest {
    private val behandlingsstatistikkMediator = mockk<BehandlingsstatistikkMediator> {
        every { hentSaksbehandlingsstatistikk() } returns toSpeilMap(
            BehandlingsstatistikkDto(
                oppgaverTilGodkjenning = BehandlingsstatistikkDto.OppgavestatistikkDto(1, listOf(FØRSTEGANGSBEHANDLING to 1)),
                tildelteOppgaver = BehandlingsstatistikkDto.OppgavestatistikkDto(0, emptyList()),
                fullførteBehandlinger = BehandlingsstatistikkDto.BehandlingerDto(0, 0, 0, 0)
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
                assertEquals(0, deserialized.fullførteBehandlinger.totalt)
                assertEquals(0, deserialized.fullførteBehandlinger.manuelt)
                assertEquals(0, deserialized.fullførteBehandlinger.automatisk)
                assertEquals(0, deserialized.fullførteBehandlinger.annulleringer)
                assertEquals(0, deserialized.antallTildelteOppgaver.totalt)
                assertEquals(0, deserialized.antallTildelteOppgaver.perPeriodetype.size)
                assertEquals(1, deserialized.antallOppgaverTilGodkjenning.totalt)
                assertEquals(1, deserialized.antallOppgaverTilGodkjenning.perPeriodetype.size)
                assertEquals(PeriodetypeForSpeil.FØRSTEGANGSBEHANDLING, deserialized.antallOppgaverTilGodkjenning.perPeriodetype.first().periodetypeForSpeil)
                assertEquals(1, deserialized.antallOppgaverTilGodkjenning.perPeriodetype.first().antall)
            }
        }
    }
}
