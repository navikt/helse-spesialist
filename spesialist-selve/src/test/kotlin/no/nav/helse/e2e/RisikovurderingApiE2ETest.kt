package no.nav.helse.e2e

import AbstractE2ETest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.api.AbstractApiTest
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.oppgaveApi
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.oppgave.OppgaveMediator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

private class RisikovurderingApiE2ETest : AbstractE2ETest() {
    private companion object {
        private val SAKSBEHANDLER_ID = UUID.randomUUID()
    }

    @Test
    suspend fun `saksbehandler medlem av risk gruppe skal se riskqa-oppgaver`() {
        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns SNAPSHOT_UTEN_WARNINGS

        val funn1 = listOf(Risikofunn(
            kategori = listOf("8-4"),
            beskrivele = "ny sjekk ikke ok",
            kreverSupersaksbehandler = true
        ))

        val funn2 = listOf(Risikofunn(
            kategori = listOf("8-4"),
            beskrivele = "8-4 ikke ok",
            kreverSupersaksbehandler = false
        ))

        godkjenningsoppgave(funn1, VEDTAKSPERIODE_ID, UTBETALING_ID)
        godkjenningsoppgave(funn2, UUID.randomUUID(), UTBETALING_ID2)

        val riskQaGruppe = UUID.randomUUID()
        val kode7Gruppe = UUID.randomUUID()
        val respons =
            AbstractApiTest.TestServer {
                oppgaveApi(
                    OppgaveMediator(oppgaveDao, tildelingDao, reservasjonDao, opptegnelseDao), riskQaGruppe, kode7Gruppe
                )
            }
                .withAuthenticatedServer {
                    it.prepareGet("/api/oppgaver") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        authentication(SAKSBEHANDLER_ID, riskQaGruppe.toString())
                    }
                }.execute()

        Assertions.assertEquals(HttpStatusCode.OK, respons.status)
        val json = runBlocking {
            objectMapper.readTree(respons.bodyAsText())
        }

        assertEquals(listOf("RISK_QA", "SØKNAD"), json.map { it["oppgavetype"].asText() })
    }

    fun godkjenningsoppgave(
        funn: List<Risikofunn>,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE_ID,
        utbetalingId: UUID = UTBETALING_ID
    ) {
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            ORGNR,
            vedtaksperiodeId,
            utbetalingId
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, vedtaksperiodeId)
        sendArbeidsgiverinformasjonløsning(
            hendelseId = godkjenningsmeldingId,
            orgnummer = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId
        )
        sendArbeidsforholdløsning(
            hendelseId = godkjenningsmeldingId,
            orgnr = ORGNR,
            vedtaksperiodeId = vedtaksperiodeId
        )
        sendEgenAnsattløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erEgenAnsatt = false
        )
        sendVergemålløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = vedtaksperiodeId,
            kanGodkjennesAutomatisk = false,
            funn = funn
        )
    }
}
