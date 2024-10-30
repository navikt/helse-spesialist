package no.nav.helse.spesialist.api.påvent

import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.api.graphql.schema.PaVent
import java.util.UUID
import javax.sql.DataSource

class PåVentApiDao(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    fun hentAktivPåVent(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT frist, saksbehandler_ref FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull {
            PaVent(
                frist = it.localDate("frist"),
                oid = it.uuid("saksbehandler_ref"),
            )
        }
}
