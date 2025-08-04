package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.NotatRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID

internal class PgNotatRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    NotatRepository {
    override fun lagre(notat: Notat) {
        if (!notat.harFåttTildeltId()) {
            insertNotat(notat).let(::NotatId).let(notat::tildelId)
        } else {
            updateNotat(notat)
        }
    }

    override fun finn(id: NotatId): Notat? =
        asSQL(
            """
            SELECT * FROM notat
            WHERE id = :notatId
            AND type NOT IN ('PaaVent', 'Retur')
            """.trimIndent(),
            "notatId" to id.value,
        ).singleOrNull { it.toNotat() }

    override fun finnAlleForVedtaksperiode(vedtaksperiodeId: UUID): List<Notat> =
        asSQL(
            """
            SELECT * FROM notat
            WHERE vedtaksperiode_id = :vedtaksperiode_id::uuid
            AND type NOT IN ('PaaVent', 'Retur')
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).list { it.toNotat() }

    private fun insertNotat(notat: Notat): Int =
        asSQL(
            """
            INSERT INTO notat (tekst, opprettet, saksbehandler_oid, vedtaksperiode_id, feilregistrert, feilregistrert_tidspunkt, type, dialog_ref)
            VALUES (:tekst, :opprettet, :saksbehandler_oid, :vedtaksperiode_id, :feilregistrert, :feilregistrert_tidspunkt, CAST(:type as notattype), :dialog_ref)
            """.trimIndent(),
            "tekst" to notat.tekst,
            "opprettet" to notat.opprettetTidspunkt,
            "saksbehandler_oid" to notat.saksbehandlerOid.value,
            "vedtaksperiode_id" to notat.vedtaksperiodeId,
            "feilregistrert" to notat.feilregistrert,
            "feilregistrert_tidspunkt" to notat.feilregistrertTidspunkt,
            "type" to notat.type.name,
            "dialog_ref" to notat.dialogRef.value,
        ).updateAndReturnGeneratedKey().toInt()

    private fun updateNotat(notat: Notat) {
        asSQL(
            """
            UPDATE notat SET
            tekst = :tekst,
            saksbehandler_oid = :saksbehandler_oid,
            vedtaksperiode_id = :vedtaksperiode_id,
            feilregistrert = :feilregistrert,
            feilregistrert_tidspunkt = :feilregistrert_tidspunkt,
            type = CAST(:type as notattype),
            dialog_ref = :dialog_ref
            WHERE id = :id
            """.trimIndent(),
            "tekst" to notat.tekst,
            "saksbehandler_oid" to notat.saksbehandlerOid.value,
            "vedtaksperiode_id" to notat.vedtaksperiodeId,
            "feilregistrert" to notat.feilregistrert,
            "feilregistrert_tidspunkt" to notat.feilregistrertTidspunkt,
            "type" to notat.type.name,
            "dialog_ref" to notat.dialogRef.value,
            "id" to notat.id().value,
        ).update()
    }

    private fun Row.toNotat(): Notat =
        Notat.Factory.fraLagring(
            id = NotatId(int("id")),
            type = NotatType.valueOf(string("type")),
            tekst = string("tekst"),
            dialogRef = DialogId(long("dialog_ref")),
            vedtaksperiodeId = UUID.fromString(string("vedtaksperiode_id")),
            saksbehandlerOid = SaksbehandlerOid(UUID.fromString(string("saksbehandler_oid"))),
            opprettetTidspunkt = localDateTime("opprettet"),
            feilregistrert = boolean("feilregistrert"),
            feilregistrertTidspunkt = localDateTimeOrNull("feilregistrert_tidspunkt"),
        )
}
