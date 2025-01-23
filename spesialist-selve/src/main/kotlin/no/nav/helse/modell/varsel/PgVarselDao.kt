package no.nav.helse.modell.varsel

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

internal interface VarselDao {
    fun avvikleVarsel(
        varselkode: String,
        definisjonId: UUID,
    )
}

class PgVarselDao(queryRunner: QueryRunner) : VarselDao, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

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
