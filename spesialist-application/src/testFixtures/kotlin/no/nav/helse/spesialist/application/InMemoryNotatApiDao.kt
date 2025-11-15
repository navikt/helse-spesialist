package no.nav.helse.spesialist.application

import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.spesialist.api.notat.KommentarDto
import java.util.UUID

class InMemoryNotatApiDao : NotatApiDao {
    override fun finnNotater(vedtaksperiodeId: UUID): List<NotatApiDao.NotatDto> {
        TODO("Not yet implemented")
    }

    override fun finnKommentarer(dialogRef: Long): List<KommentarDto> {
        TODO("Not yet implemented")
    }
}
