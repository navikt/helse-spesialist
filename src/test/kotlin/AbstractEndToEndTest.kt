import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.TestHendelse
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.tildeling.TildelingDao
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractEndToEndTest {
    protected lateinit var dataSource: DataSource
    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    protected val testRapid = TestRapid()
    internal companion object {
        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
        internal val HENDELSE_ID = UUID.randomUUID()
        internal val CONTEXT_ID = UUID.randomUUID()

        internal val VEDTAKSPERIODE = UUID.randomUUID()

        internal const val ORGNUMMER = "123456789"
        internal const val ORGNAVN = "NAVN AS"

        internal const val FNR = "12345678911"
        internal const val AKTØR = "4321098765432"
        internal const val FORNAVN = "Kari"
        internal const val MELLOMNAVN = "Mellomnavn"
        internal const val ETTERNAVN = "Nordmann"
        internal val FØDSELSDATO = LocalDate.EPOCH
        internal val KJØNN = Kjønn.Kvinne
        internal const val ENHET = "0301"

        internal val FOM = LocalDate.of(2018, 1, 1)

        internal val TOM = LocalDate.of(2018, 1, 31)
        internal val SAKSBEHANDLER_OID = UUID.randomUUID()

        internal val SAKSBEHANDLEREPOST = "sara.saksbehandler@nav.no"

        internal val TESTHENDELSE = TestHendelse(HENDELSE_ID, UUID.randomUUID(), FNR)
    }

    private var personId: Int = -1
    private var arbeidsgiverId: Int = -1
    private var snapshotId: Int = -1
    private var vedtakId: Long = -1
    internal var oppgaveId: Long = -1
        private set

    internal lateinit var personDao: PersonDao
    internal lateinit var oppgaveDao: OppgaveDao
    internal lateinit var arbeidsgiverDao: ArbeidsgiverDao
    internal lateinit var snapshotDao: SnapshotDao
    internal lateinit var vedtakDao: VedtakDao
    internal lateinit var commandContextDao: CommandContextDao
    internal lateinit var tildelingDao: TildelingDao
    internal lateinit var saksbehandlerDao: SaksbehandlerDao

    @BeforeAll
    fun setupAllE2E(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

        dataSource = HikariDataSource(hikariConfig)
        personDao = PersonDao(dataSource)
        oppgaveDao = OppgaveDao(dataSource)
        arbeidsgiverDao = ArbeidsgiverDao(dataSource)
        snapshotDao = SnapshotDao(dataSource)
        vedtakDao = VedtakDao(dataSource)
        commandContextDao = CommandContextDao(dataSource)
        tildelingDao = TildelingDao(dataSource)
        saksbehandlerDao = SaksbehandlerDao(dataSource)
    }

    @BeforeEach
    internal fun setupEachE2E() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
            .load()
            .also {
                it.clean()
                it.migrate()
            }
        testRapid.reset()
    }

    @AfterAll
    internal fun teardown() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    protected fun testbehov(hendelseId: UUID, type: String = "Godkjenningsbehov", vedtaksperiodeId: UUID = VEDTAKSPERIODE) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO spleisbehov(id, data, original, spleis_referanse, type) VALUES(?, ?::json, ?::json, ?, ?)",
                    hendelseId,
                    "{}",
                    "{}",
                    vedtaksperiodeId,
                    type
                ).asExecute
            )
        }
    }

    protected fun nyPerson() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
    }

    protected fun opprettPerson(fødselsnummer: String = FNR, aktørId: String = AKTØR): Persondata {
        val personinfoId = personDao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        val infotrygdutbetalingerId = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val enhetId = ENHET.toInt()
        personId = personDao.insertPerson(fødselsnummer, aktørId, personinfoId, enhetId, infotrygdutbetalingerId)!!
        return Persondata(personId, personinfoId, enhetId, infotrygdutbetalingerId)
    }

    protected fun opprettArbeidsgiver(organisasjonsnummer: String = ORGNUMMER, navn: String = ORGNAVN): Int {
        return arbeidsgiverDao.insertArbeidsgiver(organisasjonsnummer, navn)!!.also { arbeidsgiverId = it }
    }

    protected fun opprettVedtaksperiode(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fom: LocalDate = FOM,
        tom: LocalDate = TOM
    ): Long {
        snapshotId = snapshotDao.insertSpeilSnapshot("{}")
        return vedtakDao.upsertVedtak(vedtaksperiodeId, fom, tom, personId, arbeidsgiverId, snapshotId).also { vedtakId = it }
    }

    protected fun opprettOppgave(saksbehandleroid: UUID = SAKSBEHANDLER_OID) {
        oppgaveId = oppgaveDao.insertOppgave(HENDELSE_ID, CONTEXT_ID, "OPPGAVE", Oppgavestatus.AvventerSaksbehandler, null, saksbehandleroid, vedtakId)
    }

    protected data class Persondata(
        val personId: Int,
        val personinfoId: Int,
        val infotrygdutbetalingerId: Int,
        val enhetId: Int
    )
}
