package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.application.VarselRepository
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VarseldefinisjonId

class PgVarselRepository private constructor(
    private val dbQuery: DbQuery,
) : VarselRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))

    override fun finn(varselId: VarselId): Varsel? =
        dbQuery.singleOrNull(
            """
                SELECT sv.unik_id, sv.status, b.unik_id as behandling_unik_id, b.spleis_behandling_id, sb.oid, sv.status_endret_tidspunkt, sv.kode, avd.unik_id as definisjon_id, sv.opprettet FROM selve_varsel sv 
                JOIN behandling b ON sv.generasjon_ref = b.id
                LEFT JOIN api_varseldefinisjon avd ON sv.definisjon_ref = avd.id
                LEFT JOIN saksbehandler sb ON sv.status_endret_ident = sb.ident
                WHERE sv.unik_id = :unikId
            """,
            "unikId" to varselId.value,
        ) { row ->
            row.mapTilVarsel()
        }

    override fun finnVarsler(behandlingIder: List<SpleisBehandlingId>): List<Varsel> =
        dbQuery.listWithListParameter(
            """
               SELECT sv.unik_id, b.spleis_behandling_id, b.unik_id as behandling_unik_id, sv.status, sb.oid, sv.status_endret_tidspunkt, sv.kode, avd.unik_id as definisjon_id, sv.opprettet FROM selve_varsel sv 
                JOIN behandling b ON sv.generasjon_ref = b.id
                LEFT JOIN api_varseldefinisjon avd ON sv.definisjon_ref = avd.id
                LEFT JOIN saksbehandler sb ON sv.status_endret_ident = sb.ident
                WHERE b.spleis_behandling_id IN (${behandlingIder.joinToString { "?" }})
            """,
            *behandlingIder.map { it.value }.toTypedArray(),
        ) { row ->
            row.mapTilVarsel()
        }

    override fun finnVarslerFor(behandlingUnikIder: List<BehandlingUnikId>): List<Varsel> =
        dbQuery.listWithListParameter(
            """
                SELECT sv.unik_id, b.spleis_behandling_id, b.unik_id as behandling_unik_id, sv.status, sb.oid, sv.status_endret_tidspunkt, sv.kode, avd.unik_id as definisjon_id, sv.opprettet FROM selve_varsel sv 
                JOIN behandling b ON sv.generasjon_ref = b.id
                LEFT JOIN api_varseldefinisjon avd ON sv.definisjon_ref = avd.id
                LEFT JOIN saksbehandler sb ON sv.status_endret_ident = sb.ident
                WHERE b.unik_id IN (${behandlingUnikIder.joinToString { "?" }})
            """,
            *behandlingUnikIder.map { it.value }.toTypedArray(),
        ) { row ->
            row.mapTilVarsel()
        }

    private fun Row.mapTilVarsel(): Varsel {
        val status = enumValueOf<Varsel.Status>(string("status"))
        return Varsel.fraLagring(
            id = VarselId(uuid("unik_id")),
            spleisBehandlingId = uuidOrNull("spleis_behandling_id")?.let { SpleisBehandlingId(it) },
            behandlingUnikId = BehandlingUnikId(uuid("behandling_unik_id")),
            status = status,
            vurdering =
                if (status in listOf(Varsel.Status.VURDERT, Varsel.Status.GODKJENT)) {
                    Varselvurdering(
                        saksbehandlerId = SaksbehandlerOid(uuid("oid")),
                        tidspunkt = localDateTime("status_endret_tidspunkt"),
                        vurdertDefinisjonId = VarseldefinisjonId(uuid("definisjon_id")),
                    )
                } else {
                    null
                },
            kode = string("kode"),
            opprettetTidspunkt = localDateTime("opprettet"),
        )
    }

    override fun lagre(varsel: Varsel) {
        dbQuery.update(
            """
            UPDATE selve_varsel 
            SET status = :status, status_endret_ident = sb.ident, status_endret_tidspunkt = :tidspunkt
            FROM saksbehandler sb WHERE sb.oid = :saksbehandler
            AND unik_id = :unik_id
            """.trimIndent(),
            "status" to varsel.status.toString(),
            "saksbehandler" to varsel.vurdering?.saksbehandlerId?.value,
            "tidspunkt" to varsel.vurdering?.tidspunkt,
            "unik_id" to varsel.id().value,
        )
    }

    override fun lagre(varsler: List<Varsel>) {
        varsler.forEach { lagre(it) }
    }
}
