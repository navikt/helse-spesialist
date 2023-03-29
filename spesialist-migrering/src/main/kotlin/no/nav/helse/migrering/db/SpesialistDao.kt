package no.nav.helse.migrering.db

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.migrering.domene.IPersonObserver
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

internal class SpesialistDao(private val dataSource: DataSource): IPersonObserver {

    internal fun finnVedtakSomMangler(vedtaksperiodeIder: List<UUID>): List<UUID> {
        @Language("PostgreSQL")
        val query = "SELECT vedtaksperiode_id FROM generasjon_mangler_vedtak WHERE vedtaksperiode_id IN (${vedtaksperiodeIder.joinToString { "?" }})"
        return sessionOf(dataSource).use {
            it.run(queryOf(query, *vedtaksperiodeIder.toTypedArray()).map { it.uuid("vedtaksperiode_id") }.asList)
        }
    }

    override fun personOpprettet(aktørId: String, fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO person(fodselsnummer, aktor_id) VALUES (?, ?) ON CONFLICT (fodselsnummer) DO NOTHING "
        val insertOk = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer.toLong(), aktørId.toLong()).asUpdate) > 0
        }
        if (insertOk) sikkerlogg.info(
            "Opprettet person med {}, {}",
            kv("fødselsnummer", fødselsnummer),
            kv("aktørId", aktørId)
        )
    }

    override fun arbeidsgiverOpprettet(organisasjonsnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver(orgnummer) VALUES (?) ON CONFLICT (orgnummer) DO NOTHING "
        val insertOk = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, organisasjonsnummer.toLong()).asUpdate) > 0
        }
        if (insertOk) sikkerlogg.info(
            "Opprettet arbeidsgiver med {}",
            kv("organisasjonsnummer", organisasjonsnummer)
        )
    }

    override fun vedtaksperiodeOpprettet(
        id: UUID,
        opprettet: LocalDateTime,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        fødselsnummer: String,
        organisasjonsnummer: String,
    ) {
        opprettVedtaksperiode(id, fom, tom, organisasjonsnummer, fødselsnummer)
    }

    override fun generasjonerOppdatert(id: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
        oppdaterGenerasjonerFor(id, fom, tom, skjæringstidspunkt)
    }

    override fun vedtaksperiodeOppdaterForkastet(id: UUID, forkastet: Boolean) {
        oppdaterForkastet(id, forkastet, if (forkastet) dummyForkastetAvHendelseId else null)
    }

    private fun opprettVedtaksperiode(vedtaksperiodeId: UUID, fom: LocalDate, tom: LocalDate, organisasjonsnummer: String, fødselsnummer: String) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO vedtak(vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, forkastet) VALUES (?, ?, ?, (SELECT id FROM arbeidsgiver WHERE orgnummer = ?), (SELECT id FROM person WHERE fodselsnummer = ?), false) ON CONFLICT (vedtaksperiode_id) DO NOTHING "
        val insertOk = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, fom, tom, organisasjonsnummer.toLong(), fødselsnummer.toLong()).asUpdate) > 0
        }
        if (insertOk) sikkerlogg.info(
            "Opprettet vedtaksperiode for person {}, arbeidsgiver {}, med {}",
            kv("fødselsnummer", fødselsnummer),
            kv("organisasjonsnummer", organisasjonsnummer),
            kv("vedtaksperiodeId", vedtaksperiodeId)
        )
    }

    private fun oppdaterGenerasjonerFor(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
    ) {
        @Language("PostgreSQL")
        val query = "UPDATE selve_vedtaksperiode_generasjon SET fom = ?, tom = ?, skjæringstidspunkt = ? WHERE vedtaksperiode_id = ? AND (fom IS NULL OR tom IS NULL OR skjæringstidspunkt IS NULL) "
        val antallOppdatert = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fom, tom, skjæringstidspunkt, vedtaksperiodeId).asUpdate)
        }
        if (antallOppdatert > 0) sikkerlogg.info(
            "Oppdatert $antallOppdatert generasjoner for {}, med {}, {}, {}",
            kv("vedtaksperiodeId", vedtaksperiodeId),
            kv("fom", fom),
            kv("tom", tom),
            kv("skjæringstidspunkt", skjæringstidspunkt),
        )
    }

    private fun oppdaterForkastet(
        vedtaksperiodeId: UUID,
        forkastet: Boolean,
        forkastetAvHendelse: UUID?
    ) {
        @Language("PostgreSQL")
        val query = "UPDATE vedtak SET forkastet = ?, forkastet_av_hendelse = ? WHERE vedtaksperiode_id = ? "
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, forkastet, forkastetAvHendelse, vedtaksperiodeId).asUpdate)
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private val dummyForkastetAvHendelseId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
}