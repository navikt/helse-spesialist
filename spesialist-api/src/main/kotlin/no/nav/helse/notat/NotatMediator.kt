package no.nav.helse.notat

import java.util.*

class NotatMediator(
    private val notatDao: NotatDao,
) {

    fun lagre(vedtaksperiodeId: UUID, tekst: String, saksbehandler_oid: UUID) =
        notatDao.opprettNotat(vedtaksperiodeId, tekst, saksbehandler_oid)

    fun finn(vedtaksperiodeIds: List<UUID>) =
        notatDao.finnNotater(vedtaksperiodeIds)

}
