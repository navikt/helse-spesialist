package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import org.intellij.lang.annotations.Language

internal class WarningDao(private val dataSource: DataSource) {
    internal fun leggTilWarnings(vedtaksperiodeId: UUID, warnings: List<Warning>) {
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return
        Warning.lagre(this, warnings, vedtakRef)
    }

    internal fun leggTilWarning(vedtakRef: Long, melding: String, kilde: WarningKilde, opprettet: LocalDateTime) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                "INSERT INTO warning (melding, kilde, vedtak_ref, opprettet) VALUES (?, CAST(? as warning_kilde), ?, ?)"
            session.run(queryOf(statement, melding, kilde.name, vedtakRef, opprettet).asUpdate)
        }

    private fun finnVedtakId(vedtaksperiodeId: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
        session.run(queryOf(statement, vedtaksperiodeId).map { it.long("id") }.asSingle)
    }
}
