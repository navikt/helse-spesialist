package no.nav.helse.notat

import java.util.*

class NotatMediator(
    private val notatDao: NotatDao,
) {

    fun lagre(oppgave_ref: Int, notat: NotatDto, saksbehandler_oid: UUID) =
        notatDao.opprettNotat(oppgave_ref, notat.tekst, saksbehandler_oid)

    fun finn(oppgave_ref: Int) =
        notatDao.finnNotater(oppgave_ref)

}
