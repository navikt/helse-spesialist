package no.nav.helse.modell.automatisering

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class AutomatiseringDao(val dataSource: DataSource) {
    fun lagre(automatisert: Boolean, vedtaksperiodeId: UUID, hendelseId: UUID) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert) VALUES (?, ?, ?);",
                    finnVedtaksperiode(vedtaksperiodeId),
                    hendelseId,
                    automatisert
                ).asUpdate
            )
        }
    }

    fun hentAutomatisering(vedtaksperiodeId: UUID, hendelseId: UUID) =
        sessionOf(dataSource).use { session ->
            val vedtaksperiodeRef = finnVedtaksperiode(vedtaksperiodeId) ?: return null

            @Language("PostgreSQL")
            val query =
                """
                SELECT a.automatisert automatisert, v.vedtaksperiode_id vedtaksperiode_id, h.id hendelse_id
                FROM automatisering a
                         JOIN vedtak v ON a.vedtaksperiode_ref = v.id
                         JOIN hendelse h on h.id = a.hendelse_ref
                WHERE vedtaksperiode_ref = ? AND hendelse_ref = ?
                """
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeRef,
                    hendelseId
                ).map {
                    AutomatiseringDto(
                        automatisert = it.boolean("automatisert"),
                        vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
                        hendelseId = UUID.fromString(it.string("hendelse_id"))
                    )
                }.asSingle
            )
        }

    private fun finnVedtaksperiode(vedtaksperiodeId: UUID): Long? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?",
                vedtaksperiodeId
            ).map { it.long(1) }.asSingle
        )
    }
}

data class AutomatiseringDto(val automatisert: Boolean, val vedtaksperiodeId: UUID, val hendelseId: UUID)
