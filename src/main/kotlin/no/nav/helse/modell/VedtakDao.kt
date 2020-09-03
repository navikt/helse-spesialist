package no.nav.helse.modell

import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.findVedtak
import no.nav.helse.modell.vedtak.upsertVedtak
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {
    fun findVedtak(id: UUID) = sessionOf(dataSource).use { it.findVedtak(id) }
    fun upsertVedtak(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        personRef: Int,
        arbeidsgiverRef: Int,
        speilSnapshotRef: Int
    ) = using(sessionOf(dataSource)) {
        it.upsertVedtak(vedtaksperiodeId, fom, tom, personRef, arbeidsgiverRef, speilSnapshotRef)
    }
}
