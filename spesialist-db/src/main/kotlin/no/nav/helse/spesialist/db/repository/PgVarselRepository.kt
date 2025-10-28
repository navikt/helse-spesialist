package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.Varselvurdering
import no.nav.helse.spesialist.application.VarselRepository
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId

class PgVarselRepository private constructor(
    private val dbQuery: DbQuery,
) : VarselRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))

    override fun finnVarsler(behandlingIder: List<SpleisBehandlingId>): List<Varsel> =
        dbQuery.listWithListParameter(
            """
               SELECT sv.unik_id, b.spleis_behandling_id, sv.status, sv.status_endret_ident::uuid, sv.status_endret_tidspunkt FROM selve_varsel sv 
                JOIN behandling b ON sv.generasjon_ref = b.id
                WHERE b.spleis_behandling_id IN (${behandlingIder.joinToString { "?" }})
            """,
            *behandlingIder.map { it.value }.toTypedArray(),
        ) { row ->
            Varsel.fraLagring(
                id = VarselId(row.uuid("unik_id")),
                spleisBehandlingId = SpleisBehandlingId(row.uuid("spleis_behandling_id")),
                status = enumValueOf(row.string("status")),
                vurdering =
                    row.uuidOrNull("status_endret_ident")?.let {
                        Varselvurdering(
                            SaksbehandlerOid(it),
                            tidspunkt = row.localDateTime("status_endret_tidspunkt"),
                        )
                    },
            )
        }
}
