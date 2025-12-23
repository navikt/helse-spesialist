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

    override fun finnVarslerFor(behandlingUnikId: BehandlingUnikId): List<Varsel> =
        dbQuery.list(
            """
                SELECT sv.unik_id, b.spleis_behandling_id, b.unik_id as behandling_unik_id, sv.status, sb.oid, sv.status_endret_tidspunkt, sv.kode, avd.unik_id as definisjon_id, sv.opprettet FROM selve_varsel sv 
                JOIN behandling b ON sv.generasjon_ref = b.id
                LEFT JOIN api_varseldefinisjon avd ON sv.definisjon_ref = avd.id
                LEFT JOIN saksbehandler sb ON sv.status_endret_ident = sb.ident
                WHERE b.unik_id = :behandlingUnikId
            """,
            "behandlingUnikId" to behandlingUnikId.value,
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
                    INSERT INTO selve_varsel (unik_id, kode, status, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status_endret_ident, status_endret_tidspunkt) 
                    VALUES (
                        :unikId, 
                        :kode, 
                        :status,
                        (SELECT vedtaksperiode_id FROM behandling b WHERE b.unik_id = :behandlingUnikId), 
                        (SELECT id FROM behandling b WHERE b.unik_id = :behandlingUnikId), 
                        (SELECT id FROM api_varseldefinisjon av WHERE av.unik_id = :definisjonId),
                        :opprettet, 
                        (SELECT ident FROM saksbehandler WHERE oid = :statusEndretOid),
                        :statusEndretTidspunkt
                    )
                    ON CONFLICT (generasjon_ref, kode) DO UPDATE SET 
                        status = excluded.status,
                        status_endret_ident = excluded.status_endret_ident, 
                        status_endret_tidspunkt = excluded.status_endret_tidspunkt, 
                        definisjon_ref = excluded.definisjon_ref
                """,
            "unikId" to varsel.id.value,
            "kode" to varsel.kode,
            "status" to
                when (varsel.status) {
                    Varsel.Status.AKTIV -> "AKTIV"
                    Varsel.Status.INAKTIV -> "INAKTIV"
                    Varsel.Status.GODKJENT -> "GODKJENT"
                    Varsel.Status.VURDERT -> "VURDERT"
                    Varsel.Status.AVVIST -> "AVVIST"
                    Varsel.Status.AVVIKLET -> "AVVIKLET"
                },
            "behandlingUnikId" to varsel.behandlingUnikId.value,
            "definisjonId" to varsel.vurdering?.vurdertDefinisjonId?.value,
            "opprettet" to varsel.opprettetTidspunkt,
            "statusEndretOid" to varsel.vurdering?.saksbehandlerId?.value,
            "statusEndretTidspunkt" to varsel.vurdering?.tidspunkt,
        )
    }

    override fun lagre(varsler: List<Varsel>) {
        varsler.forEach { lagre(it) }
    }

    override fun slett(varselId: VarselId) {
        dbQuery.update(
            """
                 DELETE FROM selve_varsel WHERE unik_id = :id
            """,
            "id" to varselId.value,
        )
    }
}
