package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.varsel.Varsel.Status
import org.intellij.lang.annotations.Language

internal class VarselDao(private val dataSource: DataSource) {

    internal fun alleVarslerFor(vedtaksperiodeId: UUID): List<Varsel> {
        @Language("PostgreSQL")
        val query = "SELECT unik_id,kode,opprettet FROM selve_varsel WHERE vedtaksperiode_id = ?;"

        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(query, vedtaksperiodeId).map {
                    Varsel(
                        it.uuid("unik_id"),
                        it.string("kode"),
                        it.localDateTime("opprettet"),
                        vedtaksperiodeId
                    )
                }.asList
            )
        }
    }

    internal fun lagreVarsel(
        varselId: UUID,
        varselkode: String,
        opprettet: LocalDateTime,
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref, definisjon_ref) VALUES (?, ?, ?, ?, (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?), null) ON CONFLICT (unik_id) DO NOTHING;"

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, varselId, varselkode, vedtaksperiodeId, opprettet, generasjonId).asUpdate)
        }
    }

    internal fun oppdaterVarsel(vedtaksperiodeId: UUID, varselkode: String, status: Status, ident: String, definisjonId: UUID) {
        @Language("PostgreSQL")
        val query =
            "UPDATE selve_varsel SET status = ?,status_endret_tidspunkt = ?,status_endret_ident = ?, definisjon_ref = (SELECT id FROM api_varseldefinisjon WHERE unik_id = ?) WHERE vedtaksperiode_id = ? AND kode = ?;"

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, status.name, LocalDateTime.now(), ident, definisjonId, vedtaksperiodeId, varselkode).asUpdate)
        }
    }

    internal fun finnVarselstatus(vedtaksperiodeId: UUID, varselkode: String): Status? {
        @Language("PostgreSQL")
        val query = "SELECT status FROM selve_varsel WHERE vedtaksperiode_id = ? and kode = ?;"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, varselkode).map {
                enumValueOf<Status>(it.string(1))
            }.asSingle)
        }
    }
}