package no.nav.helse.spesialist.application

import no.nav.helse.db.DialogDao

class InMemoryDialogDao : DialogDao {
    private var counter = 0L

    override fun lagre(): Long = ++counter
}
