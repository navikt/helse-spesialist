package no.nav.helse.spesialist.api.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus
import no.nav.helse.spesialist.api.varsel.Varsel.Varselvurdering

internal class ApiVarselDao(dataSource: DataSource) : HelseDao(dataSource) {

    internal fun finnVarslerFor(vedtaksperiodeId: UUID, utbetalingId: UUID): List<Varsel> = queryize(
        """
            SELECT svg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND svg.utbetaling_id = :utbetaling_id; 
        """
    ).list(mapOf("vedtaksperiode_id" to vedtaksperiodeId, "utbetaling_id" to utbetalingId)) { mapVarsel(it) }

    internal fun finnVarslerFor(oppgaveId: Long): List<Varsel>? {
        val data = queryize(
            """
            SELECT vedtaksperiode_id, utbetaling_id FROM oppgave 
                JOIN vedtak v ON oppgave.vedtak_ref = v.id
                WHERE oppgave.id = :oppgave_id;
        """
        ).single(mapOf("oppgave_id" to oppgaveId)) { it.uuid("vedtaksperiode_id") to it.uuid("utbetaling_id") }
            ?: return null
        val (vedtaksperiodeId, utbetalingId) = data
        return finnVarslerFor(vedtaksperiodeId, utbetalingId)
    }

    internal fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
    ): Int = queryize(
        """
            UPDATE selve_varsel 
            SET 
                status = :status,
                status_endret_tidspunkt = :endret_tidspunkt,
                status_endret_ident = :endret_ident, 
                definisjon_ref = (SELECT id FROM api_varseldefinisjon WHERE unik_id = :definisjon_id) 
            WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id)
            AND kode = :kode;
        """
    ).update(
        mapOf(
            "status" to Varselstatus.VURDERT.name,
            "endret_tidspunkt" to LocalDateTime.now(),
            "endret_ident" to ident,
            "definisjon_id" to definisjonId,
            "generasjon_id" to generasjonId,
            "kode" to varselkode
        )
    )

    internal fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
    ): Int = queryize(
        """
            UPDATE selve_varsel 
            SET 
                status = :status,
                status_endret_tidspunkt = :endret_tidspunkt,
                status_endret_ident = :endret_ident, 
                definisjon_ref = null 
            WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id)
            AND kode = :kode;
        """
    ).update(
        mapOf(
            "status" to Varselstatus.AKTIV.name,
            "endret_tidspunkt" to LocalDateTime.now(),
            "endret_ident" to ident,
            "generasjon_id" to generasjonId,
            "kode" to varselkode
        )
    )

    private fun mapVarsel(it: Row): Varsel = Varsel(
        generasjonId = it.uuid("generasjon_id"),
        definisjonId = it.uuid("definisjon_id"),
        kode = it.string("kode"),
        tittel = it.string("tittel"),
        forklaring = it.stringOrNull("forklaring"),
        handling = it.stringOrNull("handling"),
        vurdering = if (it.localDateTimeOrNull("status_endret_tidspunkt") != null) Varselvurdering(
            it.string("status_endret_ident"),
            it.localDateTime("status_endret_tidspunkt"),
            Varselstatus.valueOf(it.string("status")),
        ) else null
    )
}