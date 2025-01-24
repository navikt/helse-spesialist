package no.nav.helse.db.api

import kotliquery.Query
import kotliquery.Row
import no.nav.helse.db.HelseDao
import no.nav.helse.spesialist.api.varsel.Varsel
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.GODKJENT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.INAKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.VURDERT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselvurdering
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgVarselApiDao internal constructor(dataSource: DataSource) : HelseDao(dataSource), VarselApiDao {
    private companion object {
        private val log = LoggerFactory.getLogger(PgVarselApiDao::class.java)
    }

    override fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<Varsel> =
        asSQL(
            """
            SELECT b.unik_id as generasjon_id, sv.unik_id as varsel_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN behandling b ON sv.generasjon_ref = b.id
                LEFT JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND b.utbetaling_id = :utbetaling_id AND sv.status != :status_inaktiv; 
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "utbetaling_id" to utbetalingId,
            "status_inaktiv" to INAKTIV.name,
        ).listKomplett()

    override fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) = asSQL(
        """
        SELECT b.unik_id as generasjon_id, sv.unik_id as varsel_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
            INNER JOIN behandling b ON sv.generasjon_ref = b.id
            LEFT JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
            WHERE sv.vedtaksperiode_id = :vedtaksperiode_id 
                AND sv.status != :status_inaktiv 
                AND b.id >= (
                    SELECT id FROM behandling
                    WHERE utbetaling_id = :utbetaling_id AND vedtaksperiode_id = :vedtaksperiode_id
                ); 
        """.trimIndent(),
        "vedtaksperiode_id" to vedtaksperiodeId,
        "utbetaling_id" to utbetalingId,
        "status_inaktiv" to INAKTIV.name,
    ).listKomplett()

    override fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<Varsel> =
        asSQL(
            """
            SELECT b.unik_id as generasjon_id, sv.unik_id as varsel_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv
                 INNER JOIN behandling b ON sv.generasjon_ref = b.id
                 LEFT JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                 WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND sv.status != :status_inaktiv; 
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "status_inaktiv" to INAKTIV.name,
        ).listKomplett()

    override fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<Varsel> =
        asSQL(
            """
            SELECT b.unik_id as generasjon_id, sv.unik_id as varsel_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv
                 INNER JOIN behandling b ON sv.generasjon_ref = b.id
                 LEFT JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                 WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND sv.status = :status_godkjent; 
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "status_godkjent" to GODKJENT.name,
        ).listKomplett()

    override fun godkjennVarslerFor(vedtaksperioder: List<UUID>) =
        asSQLWithQuestionMarks(
            """
            UPDATE selve_varsel 
            SET status = ? 
            WHERE status = ? 
            AND generasjon_ref IN (SELECT id FROM behandling b 
                WHERE b.vedtaksperiode_id IN (${vedtaksperioder.joinToString { "?" }}));
            """.trimIndent(),
            GODKJENT.name,
            VURDERT.name,
            *vedtaksperioder.toTypedArray(),
        ).update()

    override fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
        endretTidspunkt: LocalDateTime?,
    ): Varsel? =
        asSQL(
            """
            WITH updated AS (
                UPDATE selve_varsel 
                SET 
                    status = :status_vurdert,
                    status_endret_tidspunkt = :endret_tidspunkt,
                    status_endret_ident = :endret_ident, 
                    definisjon_ref = (SELECT id FROM api_varseldefinisjon WHERE unik_id = :definisjon_id) 
                WHERE generasjon_ref = (SELECT id FROM behandling WHERE unik_id = :generasjon_id)
                AND kode = :kode AND status NOT IN (:status_vurdert, :status_godkjent) 
                RETURNING *
            )
            SELECT u.unik_id as varsel_id, u.opprettet, u.kode, u.status, u.status_endret_ident, u.status_endret_tidspunkt, av.unik_id as definisjon_id, b.unik_id as generasjon_id, av.tittel, av.forklaring, av.handling  FROM updated u 
                INNER JOIN api_varseldefinisjon av on u.definisjon_ref = av.id
                INNER JOIN behandling b on u.generasjon_ref = b.id
            """.trimIndent(),
            "status_vurdert" to VURDERT.name,
            "status_godkjent" to GODKJENT.name,
            "endret_tidspunkt" to endretTidspunkt,
            "endret_ident" to ident,
            "definisjon_id" to definisjonId,
            "generasjon_id" to generasjonId,
            "kode" to varselkode,
        ).single(::mapVarsel)

    override fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
        endretTidspunkt: LocalDateTime?,
    ): Varsel? =
        asSQL(
            """
            WITH updated AS (
                UPDATE selve_varsel 
                SET 
                    status = :status_aktiv,
                    status_endret_tidspunkt = :endret_tidspunkt,
                    status_endret_ident = :endret_ident, 
                    definisjon_ref = null 
                WHERE generasjon_ref = (SELECT id FROM behandling WHERE unik_id = :generasjon_id)
                AND kode = :kode AND status != :status_godkjent
                RETURNING *
            )
            SELECT u.unik_id as varsel_id, u.opprettet, u.kode, u.status, u.status_endret_ident, u.status_endret_tidspunkt, av.unik_id as definisjon_id, b.unik_id as generasjon_id, av.tittel, av.forklaring, av.handling  FROM updated u 
                INNER JOIN api_varseldefinisjon av on av.id = (SELECT id FROM api_varseldefinisjon WHERE kode = u.kode ORDER BY opprettet DESC LIMIT 1)
                INNER JOIN behandling b on u.generasjon_ref = b.id
            """.trimIndent(),
            "status_aktiv" to AKTIV.name,
            "status_godkjent" to GODKJENT.name,
            "endret_tidspunkt" to endretTidspunkt,
            "endret_ident" to ident,
            "generasjon_id" to generasjonId,
            "kode" to varselkode,
        ).single(::mapVarsel)

    override fun finnStatusFor(
        varselkode: String,
        generasjonId: UUID,
    ): Varselstatus? =
        asSQL(
            """
            SELECT status FROM selve_varsel WHERE kode = :varselkode AND generasjon_ref = (SELECT id FROM behandling WHERE unik_id = :generasjon_id) 
            """.trimIndent(),
            "varselkode" to varselkode,
            "generasjon_id" to generasjonId,
        ).single { Varselstatus.valueOf(it.string("status")) }

    override fun finnVarslerFor(generasjonId: UUID): Set<Varsel> =
        asSQL(
            """
            SELECT sv.unik_id as varsel_id, b.unik_id as generasjon_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM selve_varsel sv 
                INNER JOIN behandling b ON sv.generasjon_ref = b.id
                INNER JOIN api_varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM api_varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE b.unik_id = :generasjon_id;
            """.trimIndent(),
            "generasjon_id" to generasjonId,
        ).list(::mapVarsel).toSet()

    private fun mapVarsel(it: Row): Varsel {
        val status = Varselstatus.valueOf(it.string("status"))
        return Varsel(
            varselId = it.uuid("varsel_id"),
            generasjonId = it.uuid("generasjon_id"),
            definisjonId = it.uuid("definisjon_id"),
            opprettet = it.localDateTime("opprettet"),
            kode = it.string("kode"),
            status = Varselstatus.valueOf(it.string("status")),
            tittel = it.string("tittel"),
            forklaring = it.stringOrNull("forklaring"),
            handling = it.stringOrNull("handling"),
            vurdering =
                if (status in listOf(VURDERT, GODKJENT)) {
                    Varselvurdering(
                        it.string("status_endret_ident"),
                        it.localDateTime("status_endret_tidspunkt"),
                    )
                } else {
                    null
                },
        )
    }

    private fun Query.listKomplett() =
        list { row -> sjekkForDefinisjonOgMapVerdier(row) }
            .filter { it.varseldefinisjon != null }
            .map { mapVarsel(it) }
            .toSet()

    private fun sjekkForDefinisjonOgMapVerdier(row: Row): TmpVarsel {
        if (row.uuidOrNull("definisjon_id") == null) {
            log.error(
                "Fant ikke varseldefinisjon for varselkode ${row.string(
                    "kode",
                )}. Det bør opprettes en definisjon for varselet i Spalten. Dersom dette finnes allerede, republiser definisjonen i Spalten.",
            )
        }
        val definisjonId = row.uuidOrNull("definisjon_id")
        val status = Varselstatus.valueOf(row.string("status"))
        return TmpVarsel(
            varselId = row.uuid("varsel_id"),
            generasjonId = row.uuid("generasjon_id"),
            opprettet = row.localDateTime("opprettet"),
            kode = row.string("kode"),
            status = status,
            varseldefinisjon =
                if (definisjonId != null) {
                    TmpVarseldefinisjon(
                        definisjonId = row.uuid("definisjon_id"),
                        tittel = row.string("tittel"),
                        forklaring = row.stringOrNull("forklaring"),
                        handling = row.stringOrNull("handling"),
                    )
                } else {
                    null
                },
            varselvurdering =
                if (status in listOf(VURDERT, GODKJENT)) {
                    Varselvurdering(
                        ident = row.string("status_endret_ident"),
                        tidsstempel = row.localDateTime("status_endret_tidspunkt"),
                    )
                } else {
                    null
                },
        )
    }

    private fun mapVarsel(tmpVarsel: TmpVarsel): Varsel {
        checkNotNull(tmpVarsel.varseldefinisjon)
        return Varsel(
            varselId = tmpVarsel.varselId,
            generasjonId = tmpVarsel.generasjonId,
            definisjonId = tmpVarsel.varseldefinisjon.definisjonId,
            opprettet = tmpVarsel.opprettet,
            kode = tmpVarsel.kode,
            status = tmpVarsel.status,
            tittel = tmpVarsel.varseldefinisjon.tittel,
            forklaring = tmpVarsel.varseldefinisjon.forklaring,
            handling = tmpVarsel.varseldefinisjon.handling,
            vurdering = tmpVarsel.varselvurdering,
        )
    }

    override fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: Varselstatus,
        saksbehandlerIdent: String,
    ): Int {
        if (gjeldendeStatus == GODKJENT) return godkjennVarsel(varselId)
        return asSQL(
            """
            UPDATE selve_varsel 
            SET status = :status, status_endret_ident = :saksbehandlerIdent, status_endret_tidspunkt = :endretTidspunkt
            WHERE unik_id = :varselId
            """.trimIndent(),
            "status" to gjeldendeStatus.name,
            "saksbehandlerIdent" to saksbehandlerIdent,
            "endretTidspunkt" to LocalDateTime.now(),
            "varselId" to varselId,
        ).update()
    }

    private fun godkjennVarsel(varselId: UUID) =
        asSQL(
            " UPDATE selve_varsel SET status = :status WHERE unik_id = :varselId and status_endret_ident is not null and status_endret_tidspunkt is not null ",
            "status" to GODKJENT.name,
            "varselId" to varselId,
        ).update()

    private data class TmpVarsel(
        val varselId: UUID,
        val generasjonId: UUID,
        val opprettet: LocalDateTime,
        val kode: String,
        var status: Varselstatus,
        val varseldefinisjon: TmpVarseldefinisjon?,
        val varselvurdering: Varselvurdering?,
    )

    private data class TmpVarseldefinisjon(
        val definisjonId: UUID,
        val tittel: String,
        val forklaring: String?,
        val handling: String?,
    )
}
