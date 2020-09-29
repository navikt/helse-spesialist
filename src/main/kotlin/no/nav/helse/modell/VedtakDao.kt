package no.nav.helse.modell

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.command.insertSaksbehandleroppgavetype
import no.nav.helse.modell.command.insertWarning
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.findVedtak
import no.nav.helse.modell.vedtak.upsertVedtak
import org.intellij.lang.annotations.Language
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

    internal fun opprettKobling(vedtaksperiodeId: UUID, hendelseId: UUID) = using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtaksperiode_hendelse
            SELECT id, :hendelse_id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiode_id
            ON CONFLICT DO NOTHING
        """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "hendelse_id" to hendelseId
                )
            ).asUpdate
        )
    }

    internal fun fjernKobling(vedtaksperiodeId: UUID, hendelseId: UUID) = using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
        @Language("PostgreSQL")
        val statement = """
            DELETE FROM vedtaksperiode_hendelse
            WHERE hendelse_ref = :hendelse_id AND vedtaksperiode_ref = (
                SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiode_id
            )
        """
        session.run(
            queryOf(
                statement,
                mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "hendelse_id" to hendelseId
                )
            ).asUpdate
        )
    }

    internal fun leggTilWarnings(hendelseId: UUID, meldinger: List<String>) = using(sessionOf(dataSource)) { session ->
        meldinger.forEach { melding -> session.insertWarning(melding, hendelseId) }
    }

    internal fun fjernVedtaksperioder(vedtaksperiodeIder: List<UUID>) {
        @Language("PostgreSQL")
        val statement = """
            DELETE FROM vedtak WHERE vedtaksperiode_id in (${vedtaksperiodeIder.joinToString { "?" }})
        """

        using(sessionOf(dataSource)) {
            it.run(queryOf(statement, *vedtaksperiodeIder.toTypedArray()).asUpdate)
        }
    }

    internal fun leggTilVedtaksperiodetype(hendelseId: UUID, type: Saksbehandleroppgavetype) =
        using(sessionOf(dataSource)) {
            it.insertSaksbehandleroppgavetype(type, hendelseId)
        }
}
