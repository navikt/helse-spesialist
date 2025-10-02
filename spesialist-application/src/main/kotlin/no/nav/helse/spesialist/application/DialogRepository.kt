package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.KommentarId

interface DialogRepository {
    fun lagre(dialog: Dialog)

    fun finn(id: DialogId): Dialog?

    fun finnAlle(ider: Set<DialogId>): List<Dialog>

    fun finnForKommentar(id: KommentarId): Dialog?
}
