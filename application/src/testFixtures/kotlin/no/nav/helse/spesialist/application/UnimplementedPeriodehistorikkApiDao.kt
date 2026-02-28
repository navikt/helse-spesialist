package no.nav.helse.spesialist.application

import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDto
import java.util.UUID

class UnimplementedPeriodehistorikkApiDao : PeriodehistorikkApiDao {
    override fun finn(utbetalingId: UUID): List<PeriodehistorikkDto> {
        TODO("Not yet implemented")
    }
}
