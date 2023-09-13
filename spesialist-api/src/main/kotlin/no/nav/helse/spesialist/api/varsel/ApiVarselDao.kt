package no.nav.helse.spesialist.api.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.GODKJENT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.INAKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.VURDERT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselvurdering

internal class ApiVarselDao(dataSource: DataSource) : HelseDao(dataSource) {

    internal fun finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId: UUID, utbetalingId: UUID): Set<Varsel> = asSQL(
        """
            SELECT svg.unik_id as generasjon_id, sv.unik_id as varsel_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND svg.utbetaling_id = :utbetaling_id AND sv.status != :status_inaktiv; 
        """, mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "utbetaling_id" to utbetalingId,
            "status_inaktiv" to INAKTIV.name
        )
    ).list { mapVarsel(it) }.toSet()

    internal fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId: UUID, utbetalingId: UUID) = asSQL(
        """
            SELECT svg.unik_id as generasjon_id, sv.unik_id as varsel_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id 
                    AND sv.status != :status_inaktiv 
                    AND svg.id >= (
                        SELECT id FROM selve_vedtaksperiode_generasjon
                        WHERE utbetaling_id = :utbetaling_id AND vedtaksperiode_id = :vedtaksperiode_id
                    ); 
        """, mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "utbetaling_id" to utbetalingId,
            "status_inaktiv" to INAKTIV.name
        )
    ).list { mapVarsel(it) }.toSet()

    internal fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<Varsel> = asSQL(
        """
           SELECT svg.unik_id as generasjon_id, sv.unik_id as varsel_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND sv.status != :status_inaktiv; 
        """,
        mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "status_inaktiv" to INAKTIV.name
        )
    ).list { mapVarsel(it) }.toSet()

    internal fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<Varsel> = asSQL(
        """
           SELECT svg.unik_id as generasjon_id, sv.unik_id as varsel_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND sv.status = :status_godkjent; 
        """,
        mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "status_godkjent" to GODKJENT.name
        )
    ).list { mapVarsel(it) }.toSet()

    internal fun godkjennVarslerFor(vedtaksperioder: List<UUID>) = asSQL(
        """
            UPDATE selve_varsel 
            SET status = ? 
            WHERE status = ? 
            AND generasjon_ref IN (SELECT id FROM selve_vedtaksperiode_generasjon svg 
                WHERE svg.vedtaksperiode_id IN (${vedtaksperioder.joinToString { "?" }}));
        """, GODKJENT.name, VURDERT.name, *vedtaksperioder.toTypedArray()
    ).update()

    internal fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
    ): Varsel? = asSQL(
        """
            WITH updated AS (
                UPDATE selve_varsel 
                SET 
                    status = :status_vurdert,
                    status_endret_tidspunkt = :endret_tidspunkt,
                    status_endret_ident = :endret_ident, 
                    definisjon_ref = (SELECT id FROM api_varseldefinisjon WHERE unik_id = :definisjon_id) 
                WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id)
                AND kode = :kode AND status NOT IN (:status_vurdert, :status_godkjent) 
                RETURNING *
            )
            SELECT u.unik_id as varsel_id, u.kode, u.status, u.status_endret_ident, u.status_endret_tidspunkt, av.unik_id as definisjon_id, svg.unik_id as generasjon_id, av.tittel, av.forklaring, av.handling  FROM updated u 
                INNER JOIN api_varseldefinisjon av on u.definisjon_ref = av.id
                INNER JOIN selve_vedtaksperiode_generasjon svg on u.generasjon_ref = svg.id
        """, mapOf(
            "status_vurdert" to VURDERT.name,
            "status_godkjent" to GODKJENT.name,
            "endret_tidspunkt" to LocalDateTime.now(),
            "endret_ident" to ident,
            "definisjon_id" to definisjonId,
            "generasjon_id" to generasjonId,
            "kode" to varselkode
        )
    ).single(::mapVarsel)

    internal fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
    ): Varsel? = asSQL(
        """
            WITH updated AS (
                UPDATE selve_varsel 
                SET 
                    status = :status_aktiv,
                    status_endret_tidspunkt = :endret_tidspunkt,
                    status_endret_ident = :endret_ident, 
                    definisjon_ref = null 
                WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id)
                AND kode = :kode AND status != :status_godkjent
                RETURNING *
            )
            SELECT u.unik_id as varsel_id, u.kode, u.status, u.status_endret_ident, u.status_endret_tidspunkt, av.unik_id as definisjon_id, svg.unik_id as generasjon_id, av.tittel, av.forklaring, av.handling  FROM updated u 
                INNER JOIN api_varseldefinisjon av on av.id = (SELECT id FROM api_varseldefinisjon WHERE kode = u.kode ORDER BY opprettet DESC LIMIT 1)
                INNER JOIN selve_vedtaksperiode_generasjon svg on u.generasjon_ref = svg.id
        """, mapOf(
            "status_aktiv" to AKTIV.name,
            "status_godkjent" to GODKJENT.name,
            "endret_tidspunkt" to LocalDateTime.now(),
            "endret_ident" to ident,
            "generasjon_id" to generasjonId,
            "kode" to varselkode
        )
    ).single(::mapVarsel)

    internal fun finnStatusFor(varselkode: String, generasjonId: UUID): Varselstatus? = asSQL(
        """
            SELECT status FROM selve_varsel WHERE kode = :varselkode AND generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id) 
        """, mapOf(
            "varselkode" to varselkode,
            "generasjon_id" to generasjonId,
        )
    ).single { Varselstatus.valueOf(it.string("status")) }

    internal fun finnVarslerFor(generasjonId: UUID): Set<Varsel> = asSQL(
        """
            SELECT sv.unik_id as varsel_id, svg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE svg.unik_id = :generasjon_id;
        """, mapOf("generasjon_id" to generasjonId)
    ).list(::mapVarsel).toSet()

    private fun mapVarsel(it: Row): Varsel {
        val status = Varselstatus.valueOf(it.string("status"))
        return Varsel(
            varselId = it.uuid("varsel_id"),
            generasjonId = it.uuid("generasjon_id"),
            definisjonId = it.uuid("definisjon_id"),
            kode = it.string("kode"),
            status = Varselstatus.valueOf(it.string("status")),
            tittel = it.string("tittel"),
            forklaring = it.stringOrNull("forklaring"),
            handling = it.stringOrNull("handling"),
            vurdering = if (status in listOf(VURDERT, GODKJENT)) Varselvurdering(
                it.string("status_endret_ident"),
                it.localDateTime("status_endret_tidspunkt"),
            ) else null
        )
    }

    internal fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: Varselstatus,
        saksbehandlerIdent: String,
    ): Int {
        if (gjeldendeStatus == GODKJENT) return godkjennVarsel(varselId)
        return asSQL(
            """
                UPDATE selve_varsel 
                SET status = ?, status_endret_ident = ?, status_endret_tidspunkt = ?
                WHERE unik_id = ?
            """, gjeldendeStatus.name, saksbehandlerIdent, LocalDateTime.now(), varselId
        ).update()
    }

    private fun godkjennVarsel(
        varselId: UUID,
    ) = asSQL(
        " UPDATE selve_varsel SET status = :status WHERE unik_id = :varselId and status_endret_ident is not null and status_endret_tidspunkt is not null ",
        mapOf("status" to GODKJENT.name, "varselId" to varselId)
    ).update()

}
