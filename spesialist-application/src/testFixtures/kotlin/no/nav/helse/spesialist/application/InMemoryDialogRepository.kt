package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Kommentar
import no.nav.helse.spesialist.domain.KommentarId

class InMemoryDialogRepository : DialogRepository, AbstractInMemoryRepository<DialogId, Dialog>() {
    override fun finnForKommentar(id: KommentarId): Dialog? = alle().find { id in it.kommentarer.map(Kommentar::id) }
    override fun generateId(): DialogId = DialogId((alle().maxOfOrNull { it.id().value } ?: 0) + 1)
}
