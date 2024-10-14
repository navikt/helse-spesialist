package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.modell.risiko.Risikovurdering
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID
import javax.naming.OperationNotSupportedException

class TransactionalRisikovurderingDao(private val transactionalSession: TransactionalSession) : RisikovurderingRepository {
    override fun hentRisikovurdering(vedtaksperiodeId: UUID): Risikovurdering? {
        @Language("PostgreSQL")
        val statement =
            """
            SELECT kan_godkjennes_automatisk FROM risikovurdering_2021 WHERE vedtaksperiode_id = ? ORDER BY id DESC LIMIT 1
            """.trimIndent()
        return transactionalSession.run(
            queryOf(statement, vedtaksperiodeId)
                .map { it.boolean("kan_godkjennes_automatisk") }
                .asSingle,
        )?.let(Risikovurdering::restore)
    }

    override fun kreverSupersaksbehandler(vedtaksperiodeId: UUID): Boolean = throw OperationNotSupportedException()

    override fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        kreverSupersaksbehandler: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime,
    ) {
        throw OperationNotSupportedException()
    }
}
