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

    internal fun lagreGenerasjon(
        id: UUID,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID?,
        opprettet: LocalDateTime,
        hendelseId: UUID,
        låstTidspunkt: LocalDateTime?,
    ): Boolean {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_vedtaksperiode_generasjon (unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse, låst)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (vedtaksperiode_id, utbetaling_id) DO NOTHING; 
        """
        return sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.run(
                queryOf(
                    query,
                    id,
                    vedtaksperiodeId,
                    utbetalingId,
                    opprettet,
                    hendelseId,
                    låstTidspunkt,
                    if (låstTidspunkt != null) hendelseId else null,
                    låstTidspunkt != null
                ).asUpdateAndReturnGeneratedKey
            )
        } != null
    }

    internal fun finnDefinisjonFor(melding: String): Pair<Long, String> {
        @Language("PostgreSQL")
        val query = "SELECT id, kode FROM api_varseldefinisjon WHERE tittel = regexp_replace(?, '\n|\r', ' ', 'g');"

        return requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.run(
                queryOf(
                    query,
                    melding,
                ).map { it.long("id") to it.string("kode") }.asSingle
            )
        })
    }

    internal fun lagreVarsel(
        generasjonId: UUID,
        definisjonRef: Long?,
        varselkode: String,
        varselId: UUID,
        vedtaksperiodeId: UUID,
        opprettet: LocalDateTime,
        statusEndretIdent: String?,
        statusEndretTidspunkt: LocalDateTime?,
        status: String,
    ) {
        @Language("PostgreSQL")
        val query = """
                INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref, definisjon_ref, status_endret_ident, status_endret_tidspunkt, status) 
                VALUES (?, ?, ?, ?, (SELECT id FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ?), ?, ?, ?, ?);
            """

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, varselId, varselkode, vedtaksperiodeId, opprettet, generasjonId, definisjonRef, statusEndretIdent, statusEndretTidspunkt, status).asUpdate)
        }
    }

    override fun personOpprettet(aktørId: String, fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO person(fodselsnummer, aktor_id) VALUES (?, ?) ON CONFLICT (fodselsnummer) DO NOTHING "
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer.toLong(), aktørId.toLong()).asUpdate)
        }
    }

    override fun arbeidsgiverOpprettet(organisasjonsnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver(orgnummer) VALUES (?) ON CONFLICT (orgnummer) DO NOTHING "
        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, organisasjonsnummer.toLong()).asUpdate)
        }
    }

    override fun vedtaksperiodeOpprettet(
        id: UUID,
        opprettet: LocalDateTime,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        fødselsnummer: String,
        organisasjonsnummer: String,
        forkastet: Boolean,
    ) {

        @Language("PostgreSQL")
        val query =
            "INSERT INTO vedtak(vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref) VALUES (?, ?, ?, (SELECT id FROM arbeidsgiver WHERE orgnummer = ?), (SELECT id FROM person WHERE fodselsnummer = ?)) ON CONFLICT (vedtaksperiode_id) DO NOTHING "
        val insertOk = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, id, fom, tom, organisasjonsnummer.toLong(), fødselsnummer.toLong()).asUpdate) > 0
        }
        if (insertOk) sikkerlogg.info(
            "Opprettet vedtaksperiode for person {}, arbeidsgiver {}, med {}",
            kv("fødselsnummer", fødselsnummer),
            kv("organisasjonsnummer", organisasjonsnummer),
            kv("vedtaksperiodeId", id)
        )
        oppdaterGenerasjonerFor(id, fom, tom, skjæringstidspunkt)
        oppdaterForkastet(id, forkastet, if (forkastet) dummyForkastetAvHendelseId else null)
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