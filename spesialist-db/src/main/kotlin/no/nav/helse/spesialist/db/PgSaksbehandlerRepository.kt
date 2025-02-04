package no.nav.helse.spesialist.db

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.modell.Saksbehandler
import java.util.UUID

internal class PgSaksbehandlerRepository(
    private val session: Session,
) : QueryRunner by MedSession(session), SaksbehandlerRepository {
    override fun finn(saksbehandlerId: UUID): Saksbehandler? =
        asSQL(
            """ 
            SELECT * FROM saksbehandler WHERE oid = :oid
            """.trimIndent(),
            "oid" to saksbehandlerId,
        ).singleOrNull { it.toSaksbehandler() }

    private fun Row.toSaksbehandler(): Saksbehandler =
        Saksbehandler.Factory.fraLagring(
            id = UUID.fromString(string("oid")),
            navn = string("navn"),
            epost = string("epost"),
            ident = string("ident"),
        )
}
