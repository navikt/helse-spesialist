package no.nav.helse.mediator

import io.mockk.every
import io.mockk.mockk
import io.prometheus.client.CollectorRegistry
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.Meldingssender
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.modell.person.SøknadSendtCommand
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MetrikkerFraMeldingMediatorTest : AbstractDatabaseTest() {
    private val fødselsnummer = lagFødselsnummer()

    private val testRapid = TestRapid()
    private val metrikker = CollectorRegistry.defaultRegistry

    private val kommandofabrikk = mockk<Kommandofabrikk>(relaxed = true)
    private val meldingssender = Meldingssender(testRapid)

    init {
        MeldingMediator(
            dataSource = dataSource,
            rapidsConnection = testRapid,
            kommandofabrikk = kommandofabrikk,
            avviksvurderingDao = mockk(),
            stansAutomatiskBehandlingMediator = mockk(relaxed = true),
            personService = mockk(relaxed = true),
            poisonPills = PoisonPills(emptyMap()),
        )
    }

    @Test
    fun `Registrerer tidsbruk for command`() {
        every { kommandofabrikk.søknadSendt(any(), any()) } returns
            SøknadSendtCommand(
                fødselsnummer,
                "aktørId",
                "organisasjonsnummer",
                personRepository = mockk(relaxed = true),
                inntektskilderRepository = mockk(relaxed = true),
            )

        meldingssender.sendSøknadSendt("aktørId", fødselsnummer, "organisasjonsnummer")

        val innslag =
            metrikker.getSampleValue("command_tidsbruk_count", arrayOf("command"), arrayOf("SøknadSendtCommand"))
        // Siden metrikker er globale vil tallet variere avhengig av hvor mange tester som ble kjørt
        assertTrue(innslag > 0)
    }
}
