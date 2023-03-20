package no.nav.helse.migrering.db

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.migrering.domene.Generasjon
import no.nav.helse.migrering.domene.IPersonObserver
import no.nav.helse.migrering.domene.Varsel
import org.intellij.lang.annotations.Language

internal class SpesialistDao(private val dataSource: DataSource): IPersonObserver {

    internal fun finnSisteGenerasjonFor(vedtaksperiodeId: UUID): Generasjon? {
        @Language("PostgreSQL")
        val query = "SELECT unik_id, vedtaksperiode_id, utbetaling_id, opprettet_tidspunkt, låst_tidspunkt FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY id DESC"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map {
                Generasjon(
                    it.uuid("unik_id"),
                    it.uuid("vedtaksperiode_id"),
                    it.uuidOrNull("utbetaling_id"),
                    it.localDateTime("opprettet_tidspunkt"),
                    it.localDateTimeOrNull("låst_tidspunkt"),
                    null,
                    emptyList()
                )
            }.asSingle)
        }
    }

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

    internal fun finnVarslerFor(fødselsnummer: String): List<Varsel> {
        @Language("PostgreSQL")
        val query = """
            SELECT melding, opprettet, vedtaksperiode_id, inaktiv_fra
            FROM warning 
            INNER JOIN vedtak v on warning.vedtak_ref = v.id 
                WHERE v.person_ref = (SELECT id FROM person WHERE fodselsnummer = ? LIMIT 1)
        """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer.toLong()).map {
                Varsel(
                    it.uuid("vedtaksperiode_id"),
                    it.string("melding"),
                    it.localDateTime("opprettet"),
                    UUID.randomUUID(),
                    it.localDateTimeOrNull("inaktiv_fra"),
                )
            }.asList)
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
}