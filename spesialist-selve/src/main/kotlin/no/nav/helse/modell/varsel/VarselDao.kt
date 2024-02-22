package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.modell.varsel.Varsel.Status
import org.intellij.lang.annotations.Language

internal class VarselDao(private val dataSource: DataSource) : HelseDao(dataSource) {

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

    internal fun avvikleVarsel(varselkode: String, definisjonId: UUID) {
        @Language("PostgreSQL")
        val query =
            """
                UPDATE selve_varsel 
                SET status = :avvikletStatus,
                    status_endret_tidspunkt = :endretTidspunkt,
                    status_endret_ident = :ident, 
                    definisjon_ref = (SELECT id FROM api_varseldefinisjon WHERE unik_id = :definisjonId) 
                WHERE kode = :varselkode AND status = :aktivStatus;
            """

        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "avvikletStatus" to Status.AVVIKLET.name,
                        "aktivStatus" to Status.AKTIV.name,
                        "endretTidspunkt" to LocalDateTime.now(),
                        "ident" to "avviklet_fra_speaker",
                        "definisjonId" to definisjonId,
                        "varselkode" to varselkode,
                    )
                ).asUpdate
            )
        }
    }

    internal fun oppdaterStatus(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, status: Status, ident: String?, definisjonId: UUID?) {
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
                AND kode = :varselkode
                AND status != :status;
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
            ).also {
                check(it > 0) { "Varsel $varselkode for generasjonId $generasjonId, vedtaksperiodeId $vedtaksperiodeId har allerede status $status"}
            }
        }
    }

    internal fun oppdaterGenerasjon(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {
        @Language("PostgreSQL")
        val query = """
           UPDATE selve_varsel 
           SET generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :ny_generasjon_id) 
           WHERE unik_id = :id 
           AND generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :gammel_generasjon_id)
        """

        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "ny_generasjon_id" to nyGenerasjonId,
                        "gammel_generasjon_id" to gammelGenerasjonId,
                        "id" to id,
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

    internal fun varslerFor(generasjonId: UUID): List<Varsel> {
        @Language("PostgreSQL")
        val query =
            "SELECT unik_id, vedtaksperiode_id, kode, opprettet, status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?)"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map {
                Varsel(
                    it.uuid("unik_id"),
                    it.string("kode"),
                    it.localDateTime("opprettet"),
                    it.uuid("vedtaksperiode_id"),
                    enumValueOf(it.string("status"))
                )
            }.asList)
        }
    }

    internal fun slettFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String) = asSQL(
        """
            delete from selve_varsel where vedtaksperiode_id = :vedtaksperiodeId
                and kode = :varselkode
                and generasjon_ref = (select id from selve_vedtaksperiode_generasjon where unik_id = :generasjonId) 
        """.trimIndent(), mapOf(
            "vedtaksperiodeId" to vedtaksperiodeId,
            "generasjonId" to generasjonId,
            "varselkode" to varselkode,
        )
    ).update()
}
