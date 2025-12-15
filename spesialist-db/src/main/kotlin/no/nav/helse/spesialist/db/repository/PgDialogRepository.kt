package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.DialogRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Kommentar
import no.nav.helse.spesialist.domain.KommentarId
import no.nav.helse.spesialist.domain.NAVIdent

internal class PgDialogRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    DialogRepository {
    override fun lagre(dialog: Dialog) {
        val dialogId =
            if (dialog.harFåttTildeltId()) dialog.id() else insertDialog(dialog).let(::DialogId).also(dialog::tildelId)

        dialog.kommentarer.forEach { kommentar ->
            if (!kommentar.harFåttTildeltId()) {
                insertKommentar(kommentar, dialogId).let(::KommentarId).let(kommentar::tildelId)
            } else {
                updateKommentar(kommentar, dialogId)
            }
        }
    }

    override fun finn(id: DialogId): Dialog? {
        val kommentarer =
            asSQL("SELECT * FROM kommentarer WHERE dialog_ref = :dialogId", "dialogId" to id.value)
                .list { it.tilKommentar() }
        return asSQL("SELECT * FROM dialog WHERE id = :dialogId", "dialogId" to id.value)
            .singleOrNull { it.tilDialog(kommentarer) }
    }

    override fun finnAlle(ider: Set<DialogId>): List<Dialog> {
        val kommentarerMap =
            asSQL(
                "SELECT * FROM kommentarer WHERE dialog_ref = ANY (:ider)",
                "ider" to ider.map { it.value }.toTypedArray(),
            ).list { DialogId(it.long("dialog_ref")) to it.tilKommentar() }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        return asSQL(
            "SELECT * FROM dialog WHERE id = ANY (:ider)",
            "ider" to ider.map { it.value }.toTypedArray(),
        ).list { it.tilDialog(kommentarerMap[DialogId(it.long("id"))].orEmpty()) }
    }

    override fun finnForKommentar(id: KommentarId): Dialog? =
        asSQL("SELECT dialog_ref FROM kommentarer WHERE id = :kommentarId", "kommentarId" to id.value)
            .singleOrNull { DialogId(it.long("dialog_ref")) }
            ?.let { finn(it) }

    private fun insertDialog(dialog: Dialog) =
        asSQL(
            """
            INSERT INTO dialog (opprettet)
            VALUES (:opprettet)
            """.trimIndent(),
            "opprettet" to dialog.opprettetTidspunkt,
        ).updateAndReturnGeneratedKey()

    private fun Row.tilDialog(kommentarer: List<Kommentar>) =
        Dialog.Factory.fraLagring(
            id = DialogId(long("id")),
            opprettetTidspunkt = localDateTime("opprettet"),
            kommentarer = kommentarer,
        )

    private fun insertKommentar(
        kommentar: Kommentar,
        dialogId: DialogId,
    ) = asSQL(
        """
        INSERT INTO kommentarer (tekst, feilregistrert_tidspunkt, opprettet, saksbehandlerident, dialog_ref)
        VALUES (:tekst, :feilregistrert_tidspunkt, :opprettet, :saksbehandlerident, :dialog_ref)
        """.trimIndent(),
        "tekst" to kommentar.tekst,
        "feilregistrert_tidspunkt" to kommentar.feilregistrertTidspunkt,
        "opprettet" to kommentar.opprettetTidspunkt,
        "saksbehandlerident" to kommentar.saksbehandlerident.value,
        "dialog_ref" to dialogId.value,
    ).updateAndReturnGeneratedKey().toInt()

    private fun updateKommentar(
        kommentar: Kommentar,
        dialogId: DialogId,
    ) {
        asSQL(
            """
            UPDATE kommentarer
            SET
            tekst = :tekst,
            feilregistrert_tidspunkt = :feilregistrert_tidspunkt,
            saksbehandlerident = :saksbehandlerident,
            dialog_ref = :dialog_ref
            WHERE id = :id
            """.trimIndent(),
            "tekst" to kommentar.tekst,
            "feilregistrert_tidspunkt" to kommentar.feilregistrertTidspunkt,
            "saksbehandlerident" to kommentar.saksbehandlerident.value,
            "dialog_ref" to dialogId.value,
            "id" to kommentar.id().value,
        ).update()
    }

    private fun Row.tilKommentar(): Kommentar =
        Kommentar.Factory.fraLagring(
            id = KommentarId(int("id")),
            tekst = string("tekst"),
            saksbehandlerident = NAVIdent(string("saksbehandlerident")),
            opprettetTidspunkt = localDateTime("opprettet"),
            feilregistrertTidspunkt = localDateTimeOrNull("feilregistrert_tidspunkt"),
        )
}
