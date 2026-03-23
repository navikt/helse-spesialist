package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.PåVentRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.PåVentId
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.VedtaksperiodeId

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

    override fun lagre(påVent: PåVent) {
        if (påVent.harFåttTildeltId()) {
            update(påVent)
        } else {
            insert(påVent)
                .toInt()
                .let(::PåVentId)
                .let(påVent::tildelId)
        }
    }

    private fun insert(påVent: PåVent): Long =
        asSQL(
            """
            INSERT INTO pa_vent(vedtaksperiode_id, saksbehandler_ref, frist, dialog_ref, notattekst, årsaker, opprettet) 
            VALUES (:vedtaksperiodeId, :saksbehandlerRef, :frist, :dialogRef, :notattekst, :arsaker::varchar[], :opprettet)
            """.trimIndent(),
            "vedtaksperiodeId" to påVent.vedtaksperiodeId.value,
            "saksbehandlerRef" to påVent.saksbehandlerOid.value,
            "frist" to påVent.frist,
            "dialogRef" to påVent.dialogRef?.value,
            "notattekst" to påVent.notattekst,
            "arsaker" to påVent.årsaker.somDbArray(),
            "opprettet" to påVent.opprettetTidspunkt,
        ).updateAndReturnGeneratedKey()

    private fun update(påVent: PåVent): Long =
        asSQL(
            """
            UPDATE pa_vent
             SET 
                 saksbehandler_ref = :saksbehandlerRef,
                 frist = :frist,
                 dialog_ref = :dialogRef,
                 notattekst = :notattekst,
                 årsaker = :arsaker::varchar[]
             WHERE id = :id
            """.trimIndent(),
            "saksbehandlerRef" to påVent.saksbehandlerOid.value,
            "frist" to påVent.frist,
            "dialogRef" to påVent.dialogRef?.value,
            "notattekst" to påVent.notattekst,
            "arsaker" to påVent.årsaker.somDbArray(),
            "id" to påVent.id().value,
        ).updateAndReturnGeneratedKey()

    override fun finnFor(vedtaksperiodeId: VedtaksperiodeId): PåVent? =
        asSQL(
            """
                SELECT pv.* FROM pa_vent pv 
                WHERE pv.vedtaksperiode_id = :vedtaksperiodeId""",
            "vedtaksperiodeId" to vedtaksperiodeId.value,
        ).singleOrNull {
            it.toPåVent()
        }

    private fun Row.toPåVent(): PåVent =
        PåVent.Factory.fraLagring(
            id = PåVentId(int("id")),
            vedtaksperiodeId = VedtaksperiodeId(uuid("vedtaksperiode_id")),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            frist = localDate("frist"),
            opprettetTidspunkt = instant("opprettet"),
            dialogRef = longOrNull("dialog_ref")?.let(::DialogId),
            årsaker = array<String>("årsaker").toList(),
            notattekst = stringOrNull("notattekst"),
        )
}
