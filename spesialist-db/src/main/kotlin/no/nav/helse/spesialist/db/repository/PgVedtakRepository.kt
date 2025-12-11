package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.spesialist.application.VedtakRepository
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak

class PgVedtakRepository private constructor(
    private val dbQuery: DbQuery,
) : VedtakRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))

    override fun lagre(vedtak: Vedtak) {
        dbQuery.update(
            """
                 INSERT INTO vedtak(behandling_id, fattet_automatisk, saksbehandler_ident, beslutter_ident, tidspunkt)
                 VALUES(:behandlingId, :fattetAutomatisk, :saksbehandlerIdent, :beslutterIdent, :tidspunkt)
            """,
            "behandlingId" to vedtak.id.value,
            "fattetAutomatisk" to vedtak.automatiskFattet,
            "saksbehandlerIdent" to vedtak.saksbehandlerIdent,
            "beslutterIdent" to vedtak.beslutterIdent,
            "tidspunkt" to vedtak.tidspunkt,
        )
    }

    override fun finn(spleisBehandlingId: SpleisBehandlingId): Vedtak? =
        dbQuery.singleOrNull(
            "SELECT fattet_automatisk, saksbehandler_ident, beslutter_ident, tidspunkt FROM vedtak WHERE behandling_id = :behandlingId",
            "behandlingId" to spleisBehandlingId.value,
        ) { row ->
            Vedtak.fraLagring(
                id = spleisBehandlingId,
                automatiskFattet = row.boolean("fattet_automatisk"),
                saksbehandlerIdent = row.string("saksbehandler_ident"),
                beslutterIdent = row.string("beslutter_ident"),
                tidspunkt = row.instant("tidspunkt"),
            )
        }
}
