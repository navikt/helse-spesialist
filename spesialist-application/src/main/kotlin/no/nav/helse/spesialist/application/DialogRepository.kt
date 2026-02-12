package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId

interface DialogRepository {
    fun lagre(dialog: Dialog)

    fun finn(id: DialogId): Dialog?

    fun finnAlle(ider: Set<DialogId>): List<Dialog>
}
