package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.modell.Dialog
import no.nav.helse.spesialist.modell.DialogId
import no.nav.helse.spesialist.modell.KommentarId

interface DialogRepository {
    fun lagre(dialog: Dialog)

    fun finn(id: DialogId): Dialog?

    fun finnForKommentar(id: KommentarId): Dialog?
}
