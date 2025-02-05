package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.Meldingssender
import no.nav.helse.db.TransactionalSessionFactory
import no.nav.helse.e2e.AbstractDatabaseTest
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.modell.person.SøknadSendtCommand
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
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
            sessionFactory = TransactionalSessionFactory(dataSource, TilgangskontrollForTestHarIkkeTilgang),
            personDao = repositories.personDao,
            commandContextDao = repositories.commandContextDao,
            meldingDao = repositories.meldingDao,
            meldingDuplikatkontrollDao = repositories.meldingDuplikatkontrollDao,
            kommandofabrikk = kommandofabrikk,
            dokumentDao = repositories.dokumentDao,
            varselRepository = VarselRepository(
                varselDao = repositories.varselDao,
                definisjonDao = repositories.definisjonDao
            ),
            poisonPills = PoisonPills(emptyMap()),
            env = environment,
        )
    }

    @Test
    fun `Registrerer tidsbruk for command`() {
        every { kommandofabrikk.søknadSendt(any(), any()) } returns
            SøknadSendtCommand(
                fødselsnummer,
                "aktørId",
                "organisasjonsnummer",
                personDao = mockk(relaxed = true),
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
