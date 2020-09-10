package no.nav.helse.modell

import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.command.insertSaksbehandleroppgavetype
import no.nav.helse.modell.command.insertWarning
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.findVedtak
import no.nav.helse.modell.vedtak.upsertVedtak
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class VedtakDao(private val dataSource: DataSource) {
    internal fun findVedtak(id: UUID) = sessionOf(dataSource).use { it.findVedtak(id) }

    internal fun upsertVedtak(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        personRef: Int,
        arbeidsgiverRef: Int,
        speilSnapshotRef: Int
    ) = using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.upsertVedtak(vedtaksperiodeId, fom, tom, personRef, arbeidsgiverRef, speilSnapshotRef)
    }

    internal fun leggTilWarnings(hendelseId: UUID, meldinger: List<String>) = using(sessionOf(dataSource)) { session ->
        meldinger.forEach { melding -> session.insertWarning(melding, hendelseId) }
    }

    internal fun leggTilVedtaksperiodetype(hendelseId: UUID, type: Saksbehandleroppgavetype) =
        using(sessionOf(dataSource)) {
            it.insertSaksbehandleroppgavetype(type, hendelseId)
        }
}
