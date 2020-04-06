package no.nav.helse.model

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.http.headersOf
import no.nav.helse.AccessTokenClient
import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.PersonEgenskap
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SpleisBehovTest {
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var hikariConfig: HikariConfig
    private lateinit var dataSource: HikariDataSource
    private lateinit var personDao: PersonDao
    private lateinit var arbeidsgiverDao: ArbeidsgiverDao
    private lateinit var vedtakDao: VedtakDao
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var oppgaveDao: OppgaveDao
    private lateinit var speilSnapshotRestDao: SpeilSnapshotRestDao
    private lateinit var testDao: TestPersonDao

    @BeforeAll
    fun setup() {
        embeddedPostgres = EmbeddedPostgres.builder().start()

        hikariConfig = HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

        dataSource = HikariDataSource(hikariConfig)

        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()

        personDao = PersonDao(dataSource)
        arbeidsgiverDao = ArbeidsgiverDao(dataSource)
        vedtakDao = VedtakDao(dataSource)
        snapshotDao = SnapshotDao(dataSource)
        oppgaveDao = OppgaveDao(dataSource)
        speilSnapshotRestDao = SpeilSnapshotRestDao(httpClientForSpleis, accessTokenClient, "spleisClientId")
        testDao = TestPersonDao(dataSource)
    }

    private val httpClientForSpleis = HttpClient(MockEngine) {
        install(JsonFeature) {
            this.serializer = JacksonSerializer()
        }
        engine {
            addHandler {
                respond("{}")
            }
        }
    }

    private val accessTokenClient = AccessTokenClient(
                "http://localhost.no",
        "",
        "",
        HttpClient(MockEngine) {
            install(JsonFeature) {
                this.serializer = JacksonSerializer { registerModule(JavaTimeModule()) }
            }
            engine {
                addHandler {
                    respond(content = """{"access_token": "token", "expires_on": "0"}""", headers = headersOf("Content-Type" to listOf("application/json")))
                }
            }
        }
    )


    @Test
    fun `godkjenningsbehov for ny person legger inn ny person i DB`() {
        val vedtaksperiodeId = randomUUID()
        val spleisBehov = SpleisBehov(
            id = randomUUID(),
            fødselsnummer = "12345",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "123455",
            orgnummer = "98765432",
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", PersonEgenskap.Kode6))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        spleisBehov.execute()

        assertNotNull(personDao.finnPerson(12345))
    }

    @Test
    fun `godkjenningsbehov for person oppdaterer person i DB`() {
        val vedtaksperiodeId = randomUUID()
        val spleisBehov = SpleisBehov(
            id = randomUUID(),
            fødselsnummer = "12345",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "123455",
            orgnummer = "98765432",
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", PersonEgenskap.Kode6))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        testDao.setEnhetOppdatert(12345, LocalDate.of(2020, 1, 1))
        spleisBehov.execute()
        spleisBehov.fortsett(HentEnhetLøsning("3117"))

        assertNotNull(personDao.finnPerson(12345))
        assertEquals(LocalDate.now(), personDao.enhetSistOppdatert(12345))
    }

    @Test
    fun `godkjenningsbehov for person med ny arbeidsgiver legger inn ny arbeidsgiver i DB`() {
        val vedtaksperiodeId = randomUUID()
        val spleisBehov = SpleisBehov(
            id = randomUUID(),
            fødselsnummer = "23456",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "123455",
            orgnummer = "98765432",
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", PersonEgenskap.Kode6))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        spleisBehov.execute()

        spleisBehov.fortsett(ArbeidsgiverLøsning("NAV IKT"))
        assertNotNull(arbeidsgiverDao.finnArbeidsgiver(98765432))
    }

    @Test
    fun `Ved nytt godkjenningsbehov opprettes et nytt vedtak i DB`() {
        val vedtaksperiodeId = randomUUID()
        val spleisBehov = SpleisBehov(
            id = randomUUID(),
            fødselsnummer = "34567",
            periodeFom = LocalDate.of(2018, 1, 1),
            periodeTom = LocalDate.of(2018, 1, 20),
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = "123455",
            orgnummer = "98765433",
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao
        )
        spleisBehov.execute()
        spleisBehov.fortsett(HentPersoninfoLøsning("Test", "Mellomnavn", "Etternavnsen", PersonEgenskap.Kode6))
        spleisBehov.fortsett(HentEnhetLøsning("3417"))
        spleisBehov.execute()
        spleisBehov.fortsett(ArbeidsgiverLøsning("NAV IKT"))
        spleisBehov.execute()

        assertNotNull(vedtakDao.finnVedtaksperiode(vedtaksperiodeId))

    }
}
