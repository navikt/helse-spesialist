package no.nav.helse.spesialist.api.varsel

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.GODKJENT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.INAKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.VURDERT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselvurdering
import org.intellij.lang.annotations.Language

internal class ApiVarselDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    internal fun finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId: UUID, utbetalingId: UUID): Set<Varsel> = queryize(
        """
            SELECT svg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND svg.utbetaling_id = :utbetaling_id AND sv.status != :status_inaktiv; 
        """
    ).list(
        mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "utbetaling_id" to utbetalingId,
            "status_inaktiv" to INAKTIV.name
        )
    ) { mapVarsel(it) }.toSet()

    internal fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId: UUID, utbetalingId: UUID): Set<Varsel> = queryize(
        """
            SELECT svg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id 
                    AND sv.status != :status_inaktiv 
                    AND svg.id >= (
                        SELECT id FROM selve_vedtaksperiode_generasjon
                        WHERE utbetaling_id = :utbetaling_id AND vedtaksperiode_id = :vedtaksperiode_id
                    ); 
        """
    ).list(
        mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "utbetaling_id" to utbetalingId,
            "status_inaktiv" to INAKTIV.name
        )
    ) { mapVarsel(it) }.toSet()

    internal fun finnVarslerSomIkkeErInaktiveFor(oppgaveId: Long): Set<Varsel> {
        return finnUtbetalingIdFor(oppgaveId)?.let(::finnVarslerSomIkkeErInaktiveFor) ?: emptySet()
    }

    internal fun finnVarslerSomIkkeErInaktiveFor(vedtaksperioder: List<UUID>): Set<Varsel> =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                WITH siste_generasjoner as (
                    SELECT DISTINCT ON (svg.vedtaksperiode_id) svg.vedtaksperiode_id, id, unik_id
                    FROM selve_vedtaksperiode_generasjon svg
                    WHERE svg.vedtaksperiode_id in (${vedtaksperioder.joinToString { "?" }})
                    ORDER BY svg.vedtaksperiode_id, id DESC
                )
                SELECT sg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling
                FROM selve_varsel sv
                JOIN siste_generasjoner sg on sv.generasjon_ref = sg.id
                JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.status != ?;
            """
            session.run(queryOf(query, *vedtaksperioder.toTypedArray(), INAKTIV.name).map(::mapVarsel).asList).toSet()
        }

    internal fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<Varsel> = queryize(
        """
           SELECT svg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND sv.status != :status_inaktiv; 
        """
    ).list(
        mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "status_inaktiv" to INAKTIV.name
        )
    ) { mapVarsel(it) }.toSet()

    internal fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<Varsel> = queryize(
        """
           SELECT svg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND sv.status = :status_godkjent; 
        """
    ).list(
        mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "status_godkjent" to GODKJENT.name
        )
    ) { mapVarsel(it) }.toSet()

    internal fun godkjennVarslerFor(vedtaksperioder: List<UUID>) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            UPDATE selve_varsel 
            SET status = ? 
            WHERE status = ? 
            AND generasjon_ref IN (SELECT id FROM selve_vedtaksperiode_generasjon svg 
                WHERE svg.vedtaksperiode_id IN (${vedtaksperioder.joinToString { "?" }}));
        """
        session.run(queryOf(query, GODKJENT.name, VURDERT.name, *vedtaksperioder.toTypedArray()).asUpdate)
    }

    internal fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
    ): Varsel? = queryize(
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
            SELECT u.kode, u.status, u.status_endret_ident, u.status_endret_tidspunkt, av.unik_id as definisjon_id, svg.unik_id as generasjon_id, av.tittel, av.forklaring, av.handling  FROM updated u 
                INNER JOIN api_varseldefinisjon av on u.definisjon_ref = av.id
                INNER JOIN selve_vedtaksperiode_generasjon svg on u.generasjon_ref = svg.id
        """
    ).single(
        mapOf(
            "status_vurdert" to VURDERT.name,
            "status_godkjent" to GODKJENT.name,
            "endret_tidspunkt" to LocalDateTime.now(),
            "endret_ident" to ident,
            "definisjon_id" to definisjonId,
            "generasjon_id" to generasjonId,
            "kode" to varselkode
        )
    ) {
        mapVarsel(it)
    }

    internal fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
    ): Varsel? = queryize(
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
            SELECT u.kode, u.status, u.status_endret_ident, u.status_endret_tidspunkt, av.unik_id as definisjon_id, svg.unik_id as generasjon_id, av.tittel, av.forklaring, av.handling  FROM updated u 
                INNER JOIN api_varseldefinisjon av on av.id = (SELECT id FROM api_varseldefinisjon WHERE kode = u.kode ORDER BY opprettet DESC LIMIT 1)
                INNER JOIN selve_vedtaksperiode_generasjon svg on u.generasjon_ref = svg.id
        """
    ).single(
        mapOf(
            "status_aktiv" to AKTIV.name,
            "status_godkjent" to GODKJENT.name,
            "endret_tidspunkt" to LocalDateTime.now(),
            "endret_ident" to ident,
            "generasjon_id" to generasjonId,
            "kode" to varselkode
        )
    ) {
        mapVarsel(it)
    }

    internal fun finnStatusFor(varselkode: String, generasjonId: UUID): Varselstatus? =
        queryize(
            """
                SELECT status FROM selve_varsel WHERE kode = :varselkode AND generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = :generasjon_id) 
                """
        ).single(
            mapOf(
                "varselkode" to varselkode,
                "generasjon_id" to generasjonId,
            )
        ) {
            Varselstatus.valueOf(it.string("status"))
        }

    private fun finnVarslerSomIkkeErInaktiveFor(utbetalingId: UUID): Set<Varsel> = queryize(
        """
            SELECT svg.unik_id as generasjon_id, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE svg.utbetaling_id = :utbetaling_id AND sv.status != :status_inaktiv;
        """
    ).list(mapOf("utbetaling_id" to utbetalingId, "status_inaktiv" to INAKTIV.name)) { mapVarsel(it) }.toSet()

    private fun finnUtbetalingIdFor(oppgaveId: Long) = queryize(
        "SELECT utbetaling_id FROM oppgave WHERE oppgave.id = :oppgave_id;"
    ).single(mapOf("oppgave_id" to oppgaveId)) { it.uuid("utbetaling_id") }

    private fun mapVarsel(it: Row): Varsel {
        val status = Varselstatus.valueOf(it.string("status"))
        return Varsel(
            generasjonId = it.uuid("generasjon_id"),
            definisjonId = it.uuid("definisjon_id"),
            kode = it.string("kode"),
            tittel = it.string("tittel"),
            forklaring = it.stringOrNull("forklaring"),
            handling = it.stringOrNull("handling"),
            vurdering = if (status in listOf(VURDERT, GODKJENT)) Varselvurdering(
                it.string("status_endret_ident"),
                it.localDateTime("status_endret_tidspunkt"),
                Varselstatus.valueOf(it.string("status")),
            ) else null
        )
    }
}