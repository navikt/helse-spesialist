package no.nav.helse.spesialist.db

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.application.DialogRepository
import no.nav.helse.spesialist.modell.Dialog
import no.nav.helse.spesialist.modell.Kommentar

internal class PgDialogRepository(
    private val session: Session,
) : QueryRunner by MedSession(session), DialogRepository {
    override fun lagre(dialog: Dialog) {
        val dialogId = if (dialog.harFåttTildeltId()) dialog.id() else insertDialog(dialog).also(dialog::tildelId)

        dialog.kommentarer.forEach { kommentar ->
            if (!kommentar.harFåttTildeltId()) {
                insertKommentar(kommentar, dialogId).let(kommentar::tildelId)
            } else {
                updateKommentar(kommentar, dialogId)
            }
        }
    }

    override fun finn(dialogId: Long): Dialog? {
        val kommentarer =
            asSQL("SELECT * FROM kommentarer WHERE dialog_ref = :dialogId", "dialogId" to dialogId)
                .list { it.tilKommentar() }
        return asSQL("SELECT * FROM dialog WHERE id = :dialogId", "dialogId" to dialogId)
            .singleOrNull { it.tilDialog(kommentarer) }
    }

    override fun finnForKommentar(kommentarId: Int): Dialog? =
        asSQL("SELECT dialog_ref FROM kommentarer WHERE id = :kommentarId", "kommentarId" to kommentarId)
            .singleOrNull { it.long("dialog_ref") }
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
            id = long("id"),
            opprettetTidspunkt = localDateTime("opprettet"),
            kommentarer = kommentarer,
        )

    private fun insertKommentar(
        kommentar: Kommentar,
        dialogId: Long,
    ) = asSQL(
        """
        INSERT INTO kommentarer (tekst, feilregistrert_tidspunkt, opprettet, saksbehandlerident, dialog_ref)
        VALUES (:tekst, :feilregistrert_tidspunkt, :opprettet, :saksbehandlerident, :dialog_ref)
        """.trimIndent(),
        "tekst" to kommentar.tekst,
        "feilregistrert_tidspunkt" to kommentar.feilregistrertTidspunkt,
        "opprettet" to kommentar.opprettetTidspunkt,
        "saksbehandlerident" to kommentar.saksbehandlerident,
        "dialog_ref" to dialogId,
    ).updateAndReturnGeneratedKey().toInt()

    private fun updateKommentar(
        kommentar: Kommentar,
        dialogId: Long,
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
            "saksbehandlerident" to kommentar.saksbehandlerident,
            "dialog_ref" to dialogId,
            "id" to kommentar.id(),
        ).update()
    }

    private fun Row.tilKommentar(): Kommentar =
        Kommentar.Factory.fraLagring(
            id = int("id"),
            tekst = string("tekst"),
            saksbehandlerident = string("saksbehandlerident"),
            opprettetTidspunkt = localDateTime("opprettet"),
            feilregistrertTidspunkt = localDateTimeOrNull("feilregistrert_tidspunkt"),
        )
}
