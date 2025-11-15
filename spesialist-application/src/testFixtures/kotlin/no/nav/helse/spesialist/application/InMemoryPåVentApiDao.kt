package no.nav.helse.spesialist.application

import no.nav.helse.db.api.PåVentApiDao
import java.util.UUID

class InMemoryPåVentApiDao : PåVentApiDao {
    override fun hentAktivPåVent(vedtaksperiodeId: UUID): PåVentApiDao.PaVentDto? {
        TODO("Not yet implemented")
    }
}
