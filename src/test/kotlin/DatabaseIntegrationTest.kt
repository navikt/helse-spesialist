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
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.TestHendelse
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.ReservasjonDao
import no.nav.helse.tildeling.TildelingDao
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class DatabaseIntegrationTest {
    protected companion object {
        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
        internal val HENDELSE_ID = UUID.randomUUID()

        internal val VEDTAKSPERIODE = UUID.randomUUID()

        internal const val OPPGAVETYPE = "EN OPPGAVE"
        internal val OPPGAVESTATUS = Oppgavestatus.AvventerSaksbehandler

        internal const val ORGNUMMER = "123456789"
        internal const val ORGNAVN = "NAVN AS"

        internal const val FNR = "02345678911"
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

        internal const val SAKSBEHANDLEREPOST = "sara.saksbehandler@nav.no"

        private val postgresPath = createTempDir()
        private val embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath)
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()

        private val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres").also(::println)
            maximumPoolSize = 5
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }
        internal val dataSource = HikariDataSource(hikariConfig)
    }

    internal var personId: Int = -1
        private set
    internal var arbeidsgiverId: Int = -1
        private set
    internal var snapshotId: Int = -1
        private set
    internal var vedtakId: Long = -1
        private set
    internal var oppgaveId: Long = -1
        private set

    internal val personDao = PersonDao(dataSource)
    internal val oppgaveDao = OppgaveDao(dataSource)
    internal val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    internal val snapshotDao = SnapshotDao(dataSource)
    internal val vedtakDao = VedtakDao(dataSource)
    internal val commandContextDao = CommandContextDao(dataSource)
    internal val tildelingDao = TildelingDao(dataSource)
    internal val saksbehandlerDao = SaksbehandlerDao(dataSource)
    internal val overstyringDao = OverstyringDao(dataSource)
    internal val reservasjonDao = ReservasjonDao(dataSource)
    internal val risikovurderingDao = RisikovurderingDao(dataSource)
    internal val automatiseringDao = AutomatiseringDao(dataSource)

    @BeforeEach
    internal fun resetDatabase() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
            .load()
            .also {
                it.clean()
                it.migrate()
            }
    }

    internal fun testhendelse(
        hendelseId: UUID = HENDELSE_ID,
        vedtaksperiodeId: UUID? = VEDTAKSPERIODE,
        fødselsnummer: String = FNR,
        type: String = "GODKJENNING"
    ) = TestHendelse(hendelseId, vedtaksperiodeId, fødselsnummer).also {
        lagreHendelse(it.id, it.fødselsnummer(), type)
    }

    protected fun godkjenningsbehov(
        hendelseId: UUID,
        fødselsnummer: String = FNR
    ) {
        lagreHendelse(hendelseId, fødselsnummer, "GODKJENNING")
    }

    private fun lagreHendelse(
        hendelseId: UUID,
        fødselsnummer: String = FNR,
        type: String
    ) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO hendelse(id, fodselsnummer, data, type) VALUES(?, ?, ?::json, ?)",
                    hendelseId,
                    fødselsnummer.toLong(),
                    "{}",
                    type
                ).asExecute
            )
        }
    }

    protected fun nyPerson() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtakId = vedtakId)
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

    protected fun opprettSnapshot(personBlob: String = "{}") {
        snapshotId = snapshotDao.insertSpeilSnapshot(personBlob)
    }

    protected fun opprettVedtaksperiode(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fom: LocalDate = FOM,
        tom: LocalDate = TOM
    ): Long {
        opprettSnapshot()
        return vedtakDao.opprett(vedtaksperiodeId, fom, tom, personId, arbeidsgiverId, snapshotId)
            .let { vedtakDao.finnVedtakId(vedtaksperiodeId) }
            ?.also { vedtakId = it }
            ?: fail { "Kunne ikke opprette vedtak" }
    }

    protected fun opprettOppgave(
        hendelseId: UUID = HENDELSE_ID,
        contextId: UUID = UUID.randomUUID(),
        vedtakId: Long? = null
    ) {
        oppgaveId = oppgaveDao.opprettOppgave(
            hendelseId,
            contextId,
            OPPGAVETYPE,
            vedtakId
        )
    }

    protected data class Persondata(
        val personId: Int,
        val personinfoId: Int,
        val infotrygdutbetalingerId: Int,
        val enhetId: Int
    )
}
