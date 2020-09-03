package no.nav.helse.tildeling

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.sessionOf
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.arbeidsgiver.insertArbeidsgiver
import no.nav.helse.modell.command.findSaksbehandlerOppgaver
import no.nav.helse.modell.command.insertOppgave
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.person.insertInfotrygdutbetalinger
import no.nav.helse.modell.person.insertPerson
import no.nav.helse.modell.person.insertPersoninfo
import no.nav.helse.modell.saksbehandler.persisterSaksbehandler
import no.nav.helse.modell.vedtak.snapshot.insertSpeilSnapshot
import no.nav.helse.modell.vedtak.upsertVedtak
import no.nav.helse.objectMapper
import org.flywaydb.core.Flyway
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

fun DataSource.opprettVedtak() = sessionOf(this, returnGeneratedKey = true).use {
    val personinfo = it.insertPersoninfo("Ola", null, "Nordmann", LocalDate.now(), Kjønn.Ukjent)
    val infotrygdUtbetalinger = it.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
    val person = it.insertPerson("123456789", 987654321, personinfo, 315, infotrygdUtbetalinger)
    val arbeidsgiver = it.insertArbeidsgiver(98765432, "Boomer AS")
    val speilSnapshot = it.insertSpeilSnapshot("{}")
    it.upsertVedtak(
        vedtaksperiodeId = UUID.randomUUID(),
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        personRef = person!!.toInt(),
        arbeidsgiverRef = arbeidsgiver!!.toInt(),
        speilSnapshotRef = speilSnapshot
    )
}

fun DataSource.opprettSaksbehandler(saksbehandlerId: UUID) {
    sessionOf(this).use {
        it.persisterSaksbehandler(saksbehandlerId, "Sara Saksbehandler", "sara.saksbehandler@nav.no")
    }
}

fun DataSource.opprettSaksbehandlerOppgave(oppgavereferanse: UUID, vedtakId: Long) {
    sessionOf(this).use {
        it.insertOppgave(
            oppgavereferanse,
            "TestOppgave",
            Oppgavestatus.AvventerSaksbehandler,
            null,
            null,
            vedtakRef = vedtakId
        )
    }
}

fun DataSource.finnSaksbehandlerOppgaver() = sessionOf(this).use {
    it.findSaksbehandlerOppgaver()
}

fun EmbeddedPostgres.setupDataSource(): DataSource {
    val hikariConfig = HikariConfig().apply {
        this.jdbcUrl = getJdbcUrl("postgres", "postgres")
        maximumPoolSize = 5
        minimumIdle = 1
        idleTimeout = 10001
        connectionTimeout = 1000
        maxLifetime = 30001
    }

    return HikariDataSource(hikariConfig).also {
        Flyway
            .configure()
            .dataSource(it)
            .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
            .load()
            .also { flyway ->
                flyway.clean()
                flyway.migrate()
            }
    }
}
