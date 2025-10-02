package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.PåVentRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.PåVentId
import no.nav.helse.spesialist.domain.SaksbehandlerOid

internal class PgPåVentRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    PåVentRepository {
    override fun finnAlle(ider: Set<PåVentId>): List<PåVent> =
        if (ider.isEmpty()) {
            emptyList()
        } else {
            asSQL(
                "SELECT * FROM pa_vent WHERE id = ANY (:ider)",
                "ider" to ider.map { it.value }.toTypedArray(),
            ).list { it.toPåVent() }
        }

    private fun Row.toPåVent(): PåVent =
        PåVent.Factory.fraLagring(
            id = PåVentId(int("id")),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            frist = localDate("frist"),
            opprettetTidspunkt = instant("opprettet"),
            dialogRef = longOrNull("dialog_ref")?.let(::DialogId),
            årsaker = array<String>("årsaker").toList(),
            notattekst = stringOrNull("notattekst"),
        )
}
