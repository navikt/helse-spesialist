package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.util.UUID

internal class TransactionalPåVentDao(private val session: Session) : PåVentRepository {
    override fun erPåVent(vedtaksperiodeId: UUID): Boolean {
        @Language("PostgreSQL")
        val statement =
            """
            SELECT 1 FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent()
        return session.run(
            queryOf(
                statement,
                mapOf("vedtaksperiodeId" to vedtaksperiodeId),
            ).map { it.boolean(1) }.asSingle,
        ) ?: false
    }
}
