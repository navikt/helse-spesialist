package no.nav.helse.overstyring

import java.util.*
import javax.sql.DataSource
import kotliquery.sessionOf
import no.nav.helse.HelseDao

class OverstyrtVedtaksperiodeDao(private val dataSource: DataSource) : HelseDao(dataSource)  {

    fun erVedtaksperiodeOverstyrt(vedtaksperiodeId: UUID) = sessionOf(dataSource).use {
        """ SELECT EXISTS ( SELECT 1 FROM overstyrt_vedtaksperiode WHERE vedtaksperiode_id = :vedtaksperiode_id ) 
        """.single(mapOf("vedtaksperiode_id" to vedtaksperiodeId)) { it.boolean(1) } ?: false
    }

    fun lagreOverstyrtVedtaksperiode(vedtaksperiodeId: UUID) = sessionOf(dataSource).use {
        """ INSERT INTO overstyrt_vedtaksperiode (vedtaksperiode_id)
            VALUES (:vedtaksperiode_id)
        """.update(mapOf("vedtaksperiode_id" to vedtaksperiodeId))
    }
}
