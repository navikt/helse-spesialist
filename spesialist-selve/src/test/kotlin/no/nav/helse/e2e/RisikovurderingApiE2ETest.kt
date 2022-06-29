package no.nav.helse.e2e

import AbstractE2ETest
import io.ktor.client.request.accept
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.every
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Meldingssender.sendArbeidsforholdløsning
import no.nav.helse.Meldingssender.sendArbeidsgiverinformasjonløsning
import no.nav.helse.Meldingssender.sendDigitalKontaktinformasjonløsning
import no.nav.helse.Meldingssender.sendEgenAnsattløsning
import no.nav.helse.Meldingssender.sendGodkjenningsbehov
import no.nav.helse.Meldingssender.sendPersoninfoløsning
import no.nav.helse.Meldingssender.sendRisikovurderingløsning
import no.nav.helse.Meldingssender.sendVergemålløsning
import no.nav.helse.Meldingssender.sendÅpneGosysOppgaverløsning
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.SNAPSHOT_UTEN_WARNINGS
import no.nav.helse.Testdata.UTBETALING_ID
import no.nav.helse.Testdata.UTBETALING_ID2
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.api.AbstractApiTest
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.oppgaveApi
import no.nav.helse.mediator.meldinger.Risikofunn
import no.nav.helse.spesialist.api.oppgave.OppgaveMediator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
        val beslutterGruppe = UUID.randomUUID()
        val respons =
            AbstractApiTest.TestServer {
                oppgaveApi(
                    OppgaveMediator(oppgaveDao, tildelingDao, reservasjonDao, opptegnelseDao),
                    riskQaGruppe,
                    kode7Gruppe,
                    beslutterGruppe
                )
            }
                .withAuthenticatedServer {
                    it.prepareGet("/api/oppgaver") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Application.Json)
                        authentication(SAKSBEHANDLER_ID, riskQaGruppe.toString())
                    }
                }.execute()

        assertEquals(HttpStatusCode.OK, respons.status)
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
