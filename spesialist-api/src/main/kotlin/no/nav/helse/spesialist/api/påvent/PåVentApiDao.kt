package no.nav.helse.spesialist.api.påvent

import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.graphql.schema.PaVent
import java.util.UUID
import javax.sql.DataSource

class PåVentApiDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun hentAktivPåVent(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT frist, begrunnelse, saksbehandler_ref FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent(),
            mapOf(
                "vedtaksperiodeId" to vedtaksperiodeId,
            ),
        ).single {
            PaVent(
                frist = it.localDate("frist"),
                begrunnelse = it.stringOrNull("begrunnelse"),
                oid = it.uuid("saksbehandler_ref"),
            )
        }
}
