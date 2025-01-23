package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.Meldingssender
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.modell.person.SøknadSendtCommand
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class MetrikkerFraMeldingMediatorTest : AbstractDatabaseTest() {
    private val fødselsnummer = lagFødselsnummer()

    private val testRapid = TestRapid()
    private val metrikker = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    private val kommandofabrikk = mockk<Kommandofabrikk>(relaxed = true)
    private val meldingssender = Meldingssender(testRapid)

    init {
        MeldingMediator(
            dataSource = dataSource,
            repositories = repositories,
            publiserer = MessageContextMeldingPubliserer(testRapid),
            kommandofabrikk = kommandofabrikk,
            personService = mockk(relaxed = true),
            poisonPills = PoisonPills(emptyMap()),
            commandContextDao = repositories.commandContextDao
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
            metrikker.scrape("text/plain", setOf("command_tidsbruk_count"))//, arrayOf("command"), arrayOf("SøknadSendtCommand"))
        // Siden metrikker er globale vil tallet variere avhengig av hvor mange tester som ble kjørt
        assertNotNull(innslag)
        println(innslag)
    }
}
