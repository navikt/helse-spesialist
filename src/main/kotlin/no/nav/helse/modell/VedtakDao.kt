package no.nav.helse.modell

import kotliquery.sessionOf
import no.nav.helse.modell.vedtak.findVedtak
import java.util.*
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {
    fun findVedtak(id: UUID) = sessionOf(dataSource).use { it.findVedtak(id) }
}
