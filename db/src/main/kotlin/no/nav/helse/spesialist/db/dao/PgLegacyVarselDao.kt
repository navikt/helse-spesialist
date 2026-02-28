package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.LegacyVarselDao
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Status
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgLegacyVarselDao internal constructor(
    dataSource: DataSource,
) : LegacyVarselDao,
    QueryRunner by MedDataSource(dataSource) {
    override fun avvikleVarsel(
        varselkode: String,
        definisjonId: UUID,
    ) {
        asSQL(
            """
            UPDATE selve_varsel 
            SET status = :avvikletStatus,
                status_endret_tidspunkt = :endretTidspunkt,
                status_endret_ident = :ident, 
                definisjon_ref = (SELECT id FROM api_varseldefinisjon WHERE unik_id = :definisjonId) 
            WHERE kode = :varselkode AND status = :aktivStatus;
            """.trimIndent(),
            "avvikletStatus" to Status.AVVIKLET.name,
            "aktivStatus" to Status.AKTIV.name,
            "endretTidspunkt" to LocalDateTime.now(),
            "ident" to "avviklet_fra_speaker",
            "definisjonId" to definisjonId,
            "varselkode" to varselkode,
        ).update()
    }
}
