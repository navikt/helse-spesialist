package no.nav.helse.db.api

import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.db.api.PåVentApiDao.PaVentDto
import java.util.UUID
import javax.sql.DataSource

class PgPåVentApiDao internal constructor(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource), PåVentApiDao {
    override fun hentAktivPåVent(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT frist, saksbehandler_ref FROM pa_vent WHERE vedtaksperiode_id = :vedtaksperiodeId
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull {
            PaVentDto(
                frist = it.localDate("frist"),
                oid = it.uuid("saksbehandler_ref"),
            )
        }
}
