package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.modell.Dialog

interface DialogRepository {
    fun lagre(dialog: Dialog)

    fun finn(dialogId: Long): Dialog?

    fun finnForKommentar(kommentarId: Int): Dialog?
}
