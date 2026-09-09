package no.nav.helse.spesialist.db.dao.api

import kotliquery.Query
import kotliquery.Row
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.spesialist.db.HelseDao
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

class PgVarselApiDao internal constructor(
    dataSource: DataSource,
) : HelseDao(dataSource) {
    private companion object {
        private val log = LoggerFactory.getLogger(PgVarselApiDao::class.java)
    }

    fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDbDto> =
        asSQL(
            """
            SELECT b.unik_id as behandling_id, sv.unik_id as varsel_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM varsel sv 
                INNER JOIN behandling b ON sv.behandling_ref = b.id
                LEFT JOIN varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND b.utbetaling_id = :utbetaling_id AND sv.status != :status_inaktiv; 
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "utbetaling_id" to utbetalingId,
            "status_inaktiv" to VarselDbDto.Varselstatus.INAKTIV.name,
        ).listKomplett()

    fun finnVarslerSomIkkeErInaktiveForSisteBehandling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) = asSQL(
        """
        SELECT b.unik_id as behandling_id, sv.unik_id as varsel_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM varsel sv 
            INNER JOIN behandling b ON sv.behandling_ref = b.id
            LEFT JOIN varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
            WHERE sv.vedtaksperiode_id = :vedtaksperiode_id 
                AND sv.status != :status_inaktiv 
                AND b.id >= (
                    SELECT id FROM behandling
                    WHERE utbetaling_id = :utbetaling_id AND vedtaksperiode_id = :vedtaksperiode_id
                ); 
        """.trimIndent(),
        "vedtaksperiode_id" to vedtaksperiodeId,
        "utbetaling_id" to utbetalingId,
        "status_inaktiv" to VarselDbDto.Varselstatus.INAKTIV.name,
    ).listKomplett()

    fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> =
        asSQL(
            """
            SELECT b.unik_id as behandling_id, sv.unik_id as varsel_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM varsel sv
                 INNER JOIN behandling b ON sv.behandling_ref = b.id
                 LEFT JOIN varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                 WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND sv.status != :status_inaktiv; 
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "status_inaktiv" to VarselDbDto.Varselstatus.INAKTIV.name,
        ).listKomplett()

    fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> =
        asSQL(
            """
            SELECT b.unik_id as behandling_id, sv.unik_id as varsel_id, sv.opprettet, sv.kode, sv.status_endret_ident, sv.status_endret_tidspunkt, sv.status, av.unik_id as definisjon_id, av.tittel, av.forklaring, av.handling FROM varsel sv
                 INNER JOIN behandling b ON sv.behandling_ref = b.id
                 LEFT JOIN varseldefinisjon av ON av.id = COALESCE(sv.definisjon_ref, (SELECT id FROM varseldefinisjon WHERE kode = sv.kode ORDER BY opprettet DESC LIMIT 1))
                 WHERE sv.vedtaksperiode_id = :vedtaksperiode_id AND sv.status = :status_godkjent; 
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "status_godkjent" to VarselDbDto.Varselstatus.GODKJENT.name,
        ).listKomplett()

    private fun Query.listKomplett() =
        list { row -> sjekkForDefinisjonOgMapVerdier(row) }
            .filterNot { it.status == VarselDbDto.Varselstatus.AVVIKLET }
            .filter { it.varseldefinisjon != null }
            .toSet()

    private fun sjekkForDefinisjonOgMapVerdier(row: Row): VarselDbDto {
        if (row.uuidOrNull("definisjon_id") == null) {
            log.error(
                "Fant ikke varseldefinisjon for varselkode ${row.string(
                    "kode",
                )}. Det bør opprettes en definisjon for varselet i Spalten. Dersom dette finnes allerede, republiser definisjonen i Spalten.",
            )
        }
        val definisjonId = row.uuidOrNull("definisjon_id")
        val status = VarselDbDto.Varselstatus.valueOf(row.string("status"))
        return VarselDbDto(
            varselId = row.uuid("varsel_id"),
            behandlingId = row.uuid("behandling_id"),
            opprettet = row.localDateTime("opprettet"),
            kode = row.string("kode"),
            status = status,
            varseldefinisjon =
                if (definisjonId != null) {
                    VarselDbDto.VarseldefinisjonDbDto(
                        definisjonId = row.uuid("definisjon_id"),
                        tittel = row.string("tittel"),
                        forklaring = row.stringOrNull("forklaring"),
                        handling = row.stringOrNull("handling"),
                    )
                } else {
                    null
                },
            varselvurdering =
                if (status in
                    listOf(
                        VarselDbDto.Varselstatus.VURDERT,
                        VarselDbDto.Varselstatus.GODKJENT,
                    )
                ) {
                    VarselDbDto.VarselvurderingDbDto(
                        ident = row.string("status_endret_ident"),
                        tidsstempel = row.localDateTime("status_endret_tidspunkt"),
                    )
                } else {
                    null
                },
        )
    }
}
