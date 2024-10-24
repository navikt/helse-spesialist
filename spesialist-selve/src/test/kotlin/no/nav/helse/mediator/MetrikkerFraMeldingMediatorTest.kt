package no.nav.helse.mediator

import DatabaseIntegrationTest
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.Meldingssender
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.modell.person.PersonService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MetrikkerFraMeldingMediatorTest : DatabaseIntegrationTest() {

    private val testRapid = TestRapid()
    private val metrikker = CollectorRegistry.defaultRegistry

    private val kommandofabrikk = Kommandofabrikk(
        dataSource,
        oppgaveService = { mockk() },
        godkjenningMediator = mockk(),
        subsumsjonsmelderProvider = { mockk() },
        stikkprøver = mockk()
    )
    private val meldingssender = Meldingssender(testRapid)

    init {
        MeldingMediator(
            dataSource = dataSource,
            rapidsConnection = testRapid,
            kommandofabrikk = kommandofabrikk,
            avviksvurderingDao = mockk(),
            stansAutomatiskBehandlingMediator = mockk(relaxed = true),
            personService = PersonService(dataSource),
            poisonPills = PoisonPills(emptyMap()),
        )
    }

    @Test
    fun `Registrerer tidsbruk for command`() {
        opprettPerson()

        meldingssender.sendVedtaksperiodeNyUtbetaling(AKTØR, FNR, ORGNUMMER, VEDTAKSPERIODE, UTBETALING_ID)

        val innslag =
            metrikker.getSampleValue("command_tidsbruk_count", arrayOf("command"), arrayOf("VedtaksperiodeNyUtbetalingCommand"))
        // Siden metrikker er globale vil tallet variere avhengig av hvor mange tester som ble kjørt
        assertTrue(innslag > 0)
    }
}
