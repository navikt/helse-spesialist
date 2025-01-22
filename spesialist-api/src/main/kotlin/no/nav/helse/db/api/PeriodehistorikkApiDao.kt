package no.nav.helse.db.api

import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDto
import java.util.UUID

interface PeriodehistorikkApiDao {
    fun finn(utbetalingId: UUID): List<PeriodehistorikkDto>
}
