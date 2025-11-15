package no.nav.helse.spesialist.application

import no.nav.helse.db.DialogDao
import no.nav.helse.spesialist.domain.Dialog

class InMemoryDialogDao(private val dialogRepository: InMemoryDialogRepository) : DialogDao {
    override fun lagre(): Long = Dialog.Factory.ny().also(dialogRepository::lagre).id().value
}
