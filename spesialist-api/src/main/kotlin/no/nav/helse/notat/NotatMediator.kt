package no.nav.helse.notat

import java.util.*

class NotatMediator(
    private val notatDao: NotatDao,
) {

    fun lagre(vedtaksperiodeId: UUID, notat: NotatDto, saksbehandler_oid: UUID) =
        notatDao.opprettNotat(vedtaksperiodeId, notat.tekst, saksbehandler_oid)

    fun finn(vedtaksperiodeIds: List<UUID>) =
        notatDao.finnNotater(vedtaksperiodeIds)

}
