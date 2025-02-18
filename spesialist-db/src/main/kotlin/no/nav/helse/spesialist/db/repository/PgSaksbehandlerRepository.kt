package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.modell.Saksbehandler
import no.nav.helse.spesialist.modell.SaksbehandlerOid
import java.util.UUID

internal class PgSaksbehandlerRepository(
    session: Session,
) : QueryRunner by MedSession(session), SaksbehandlerRepository {
    override fun finn(oid: SaksbehandlerOid): Saksbehandler? =
        asSQL(
            """ 
            SELECT * FROM saksbehandler WHERE oid = :oid
            """.trimIndent(),
            "oid" to oid.value,
        ).singleOrNull { it.toSaksbehandler() }

    private fun Row.toSaksbehandler(): Saksbehandler =
        Saksbehandler.Factory.fraLagring(
            id = SaksbehandlerOid(UUID.fromString(string("oid"))),
            navn = string("navn"),
            epost = string("epost"),
            ident = string("ident"),
        )
}
