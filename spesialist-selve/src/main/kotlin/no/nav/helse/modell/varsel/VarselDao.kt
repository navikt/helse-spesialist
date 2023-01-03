package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.varsel.Varsel.Status
import org.intellij.lang.annotations.Language

internal class VarselDao(private val dataSource: DataSource) {

    internal fun lagreVarsel(
        varselId: UUID,
        varselkode: String,
        opprettet: LocalDateTime,
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, opprettet, generasjon_ref, definisjon_ref) VALUES (?, ?, ?, ?, (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?), null);"

        sessionOf(dataSource).use { session ->
            session.run(queryOf(query, varselId, varselkode, vedtaksperiodeId, opprettet, generasjonId).asUpdate)
        }
    }

    internal fun oppdaterVarsel(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, status: Status, ident: String?, definisjonId: UUID?) {
        @Language("PostgreSQL")
        val query =
            """
                UPDATE selve_varsel 
                SET 
                    status = :status,
                    status_endret_tidspunkt = :endretTidspunkt,
                    status_endret_ident = :ident, 
                    definisjon_ref = case :definisjonId 
                                when NULL then NULL 
                                else (SELECT id FROM api_varseldefinisjon WHERE unik_id = :definisjonId) 
                    end
                WHERE vedtaksperiode_id = :vedtaksperiodeId
                AND generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjonId) 
                AND kode = :varselkode;
            """

        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "status" to status.name,
                        "endretTidspunkt" to LocalDateTime.now(),
                        "ident" to ident,
                        "definisjonId" to definisjonId,
                        "vedtaksperiodeId" to vedtaksperiodeId,
                        "generasjonId" to generasjonId,
                        "varselkode" to varselkode
                    )
                ).asUpdate
            )
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