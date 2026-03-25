package no.nav.helse.mediator.påvent

import no.nav.helse.db.PåVentDao

class PåVentRepository(
    private val dao: PåVentDao,
) {
    fun fjernFraPåVent(oppgaveId: Long) {
        dao.slettPåVent(oppgaveId)
    }
}
