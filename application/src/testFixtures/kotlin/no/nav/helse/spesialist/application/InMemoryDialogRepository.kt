package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Kommentar
import no.nav.helse.spesialist.domain.KommentarId

class InMemoryDialogRepository : DialogRepository, AbstractLateIdInMemoryRepository<DialogId, Dialog>() {
    override fun tildelIderSomMangler(root: Dialog) {
        if (!root.harFåttTildeltId())
            root.tildelId(DialogId((alle().maxOfOrNull { it.id().value } ?: 0) + 1))
        root.kommentarer.forEach { kommentar ->
            if (!kommentar.harFåttTildeltId()) {
                kommentar.tildelId(KommentarId(((alle().flatMap { dialog ->
                    dialog.kommentarer.filterNot { it.harFåttTildeltId() }.map(Kommentar::id)
                }).maxOfOrNull { it.value } ?: 0) + 1))
            }
        }
    }

    override fun deepCopy(original: Dialog): Dialog = Dialog.Factory.fraLagring(
        id = original.id(),
        opprettetTidspunkt = original.opprettetTidspunkt,
        kommentarer = original.kommentarer.map { originalKommentar ->
            Kommentar.Factory.fraLagring(
                id = originalKommentar.id(),
                tekst = originalKommentar.tekst,
                saksbehandlerident = originalKommentar.saksbehandlerident,
                opprettetTidspunkt = originalKommentar.opprettetTidspunkt,
                feilregistrertTidspunkt = originalKommentar.feilregistrertTidspunkt,
            )
        },
    )
}
