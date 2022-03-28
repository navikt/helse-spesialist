package no.nav.helse.vedtaksperiode

import no.nav.helse.HelseDao
import java.util.*
import javax.sql.DataSource

class VarselDao(dataSource: DataSource): HelseDao(dataSource) {
    fun finnVarsler(vedtaksperiodeId: UUID): List<String> =
        """SELECT melding FROM warning WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)"""
            .list(mapOf("vedtaksperiodeId" to vedtaksperiodeId)) { it.string("melding") }
}
