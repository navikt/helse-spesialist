package no.nav.helse.migrering

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

internal class SpesialistDao(private val dataSource: DataSource) {

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
    ) {
        @Language("PostgreSQL")
        val query = """
                INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref, definisjon_ref) 
                VALUES (?, ?, ?, ?, (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?), ?) 
                ON CONFLICT (unik_id) DO NOTHING;
            """

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, varselId, varselkode, vedtaksperiodeId, opprettet, generasjonId, definisjonRef).asUpdate)
        }
    }
}