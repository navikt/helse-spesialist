package no.nav.helse.overstyring

import java.util.*
import javax.sql.DataSource
import kotliquery.sessionOf
import no.nav.helse.HelseDao

class OverstyrtVedtaksperiodeDao(private val dataSource: DataSource) : HelseDao(dataSource)  {

    fun hentVedtaksperiodeOverstyrtTyper(vedtaksperiodeId: UUID): List<OverstyringType> = sessionOf(dataSource).use {
        """ SELECT type FROM overstyrt_vedtaksperiode 
            WHERE vedtaksperiode_id = :vedtaksperiode_id
            AND ferdigstilt = false
        """.list(mapOf("vedtaksperiode_id" to vedtaksperiodeId)) { OverstyringType.valueOf(it.string("type")) }
    }

    fun lagreOverstyrtVedtaksperiode(vedtaksperiodeId: UUID, type: OverstyringType) = sessionOf(dataSource).use {
        """ INSERT INTO overstyrt_vedtaksperiode (vedtaksperiode_id, type)
            VALUES (:vedtaksperiode_id, :type::overstyringtype)
        """.update(mapOf("vedtaksperiode_id" to vedtaksperiodeId, "type" to type.name))
    }
}
