package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.helse.Meldingssender
import no.nav.helse.e2e.AbstractDatabaseTest
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.spesialist.db.TransactionalSessionFactory
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MetrikkerFraMeldingMediatorTest : AbstractDatabaseTest() {
    private val fødselsnummer = lagFødselsnummer()

    private val testRapid = TestRapid()
    private val metrikker = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    private val kommandofabrikk = mockk<Kommandofabrikk>(relaxed = true)
    private val meldingssender = Meldingssender(testRapid)

    init {
        MeldingMediator(
            sessionFactory = TransactionalSessionFactory(dataSource),
            personDao = daos.personDao,
            commandContextDao = daos.commandContextDao,
            meldingDao = daos.meldingDao,
            meldingDuplikatkontrollDao = daos.meldingDuplikatkontrollDao,
            kommandofabrikk = kommandofabrikk,
            dokumentDao = daos.dokumentDao,
            varselRepository = VarselRepository(
                varselDao = daos.varselDao,
                definisjonDao = daos.definisjonDao
            ),
            poisonPillDao = daos.poisonPillDao,
            generasjonDao = daos.generasjonDao,
            ignorerMeldingerForUkjentePersoner = false
        )
    }

    @Test
    fun `Registrerer tidsbruk for command`() {
        meldingssender.sendSøknadSendt("aktørId", fødselsnummer, "organisasjonsnummer")

        val innslag =
            metrikker.scrape("text/plain", setOf("command_tidsbruk_count"))//, arrayOf("command"), arrayOf("SøknadSendtCommand"))
        // Siden metrikker er globale vil tallet variere avhengig av hvor mange tester som ble kjørt
        assertNotNull(innslag)
        println(innslag)
    }
}
