package no.nav.helse.spesialist.api.varsel

import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.valueOf
import no.nav.helse.spesialist.api.varsel.Varsel.Varselvurdering
import org.intellij.lang.annotations.Language

internal class ApiVarselDao(private val dataSource: DataSource) {

    internal fun finnVarslerFor(vedtaksperiodeId: UUID, utbetalingId: UUID): List<Varsel> {
        @Language("PostgreSQL")
        val query = """
            SELECT svg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = ? AND svg.utbetaling_id = ?; 
        """

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, utbetalingId).map {
                Varsel(
                    it.uuid("generasjon_id"),
                    it.uuid("definisjon_id"),
                    it.string("kode"),
                    it.string("tittel"),
                    it.stringOrNull("forklaring"),
                    it.stringOrNull("handling"),
                    if (it.localDateTimeOrNull("status_endret_tidspunkt") != null) Varselvurdering(
                        it.string("status_endret_ident"),
                        it.localDateTime("status_endret_tidspunkt"),
                        valueOf(it.string("status")),
                    ) else null
                )
            }.asList)
        }
    }
}