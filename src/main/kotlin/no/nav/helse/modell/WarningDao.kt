package no.nav.helse.modell

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.modell.vedtak.WarningKilde
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class WarningDao(private val dataSource: DataSource) {
    internal fun leggTilWarnings(vedtaksperiodeId: UUID, warnings: List<WarningDto>) {
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return
        WarningDto.lagre(this, warnings, vedtakRef)
    }

    internal fun fjernWarnings(vedtaksperiodeId: UUID) {
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return
        using(sessionOf(dataSource)) { session ->
            @Language("PostgreSQL")
            val statement = "DELETE FROM warning WHERE vedtak_ref=?"
            session.run(queryOf(statement, vedtakRef).asExecute)
        }
    }

    private fun fjernWarnings(vedtakRef: Long, kilde: WarningKilde) {
        using(sessionOf(dataSource)) { session ->
            @Language("PostgreSQL")
            val statement = "DELETE FROM warning WHERE vedtak_ref=? AND kilde=CAST(? as warning_kilde)"
            session.run(queryOf(statement, vedtakRef, kilde.name).asExecute)
        }
    }

    internal fun oppdaterSpleisWarnings(vedtaksperiodeId: UUID, warnings: List<WarningDto>) {
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return
        fjernWarnings(vedtakRef, WarningKilde.Spleis)
        WarningDto.lagre(this, warnings, vedtakRef)
    }

    internal fun leggTilWarning(vedtaksperiodeId: UUID, warning: WarningDto) {
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return
        warning.lagre(this, vedtakRef)
    }

    internal fun leggTilWarning(vedtakRef: Long, melding: String, kilde: WarningKilde) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO warning (melding, kilde, vedtak_ref) VALUES (?, CAST(? as warning_kilde), ?)"
        session.run(queryOf(statement, melding, kilde.name, vedtakRef).asUpdate)
    }

    internal fun finnWarnings(vedtaksperiodeId: UUID): List<WarningDto> = sessionOf(dataSource).use { session ->
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return emptyList()
        @Language("PostgreSQL")
        val statement = "SELECT * FROM warning where vedtak_ref = ?"
        session.run(queryOf(statement, vedtakRef).map { WarningDto( melding = it.string("melding"), kilde = WarningKilde.valueOf(it.string("kilde"))) }.asList)
    }

    private fun finnVedtakId(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
        session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)
    }
}
