package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.modell.periodehistorikk.AutomatiskBehandlingStanset
import no.nav.helse.modell.periodehistorikk.AutomatiskBehandlingStansetAvSaksbehandler
import no.nav.helse.modell.periodehistorikk.AvventerTotrinnsvurdering
import no.nav.helse.modell.periodehistorikk.EndrePåVent
import no.nav.helse.modell.periodehistorikk.FjernetFraPåVent
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.periodehistorikk.LagtPåVent
import no.nav.helse.modell.periodehistorikk.OpphevStansAvSaksbehandler
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingAutomatiskRetur
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingFerdigbehandlet
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingRetur
import no.nav.helse.modell.periodehistorikk.VedtaksperiodeReberegnet
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import java.util.UUID
import javax.sql.DataSource

class PgPeriodehistorikkDao private constructor(
    private val queryRunner: QueryRunner,
) : PeriodehistorikkDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagreMedOppgaveId(
        historikkinnslag: Historikkinnslag,
        oppgaveId: Long,
    ) {
        val generasjonId = PgOppgaveDao(queryRunner).finnGenerasjonId(oppgaveId)
        lagre(historikkinnslag, generasjonId)
    }

    override fun lagre(
        historikkinnslag: Historikkinnslag,
        generasjonId: UUID,
    ) {
        asSQL(
            """
                INSERT INTO periodehistorikk (type, saksbehandler_oid, generasjon_id, utbetaling_id, dialog_ref, json)
                VALUES (:type, :saksbehandler_oid, :generasjon_id, null, :dialog_ref, :json::json)
        """,
            "type" to historikkinnslag.type(),
            "saksbehandler_oid" to historikkinnslag.saksbehandler?.id()?.value,
            "generasjon_id" to generasjonId,
            "dialog_ref" to historikkinnslag.dialogRef,
            "json" to historikkinnslag.detaljer().let { objectMapper.writeValueAsString(it) },
        ).update()
    }

    private fun Historikkinnslag.type() =
        when (this) {
            is LagtPåVent -> "LEGG_PA_VENT"
            is FjernetFraPåVent -> "FJERN_FRA_PA_VENT" // TODO: Mangler å migrere typen i databasen
            is TotrinnsvurderingFerdigbehandlet -> "TOTRINNSVURDERING_ATTESTERT" // TODO: Mangler å migrere typen i databasen
            is AvventerTotrinnsvurdering -> "TOTRINNSVURDERING_TIL_GODKJENNING" // TODO: Mangler å migrere typen i databasen
            is TotrinnsvurderingRetur -> "TOTRINNSVURDERING_RETUR" // TODO: Mangler å migrere typen i databasen
            is TotrinnsvurderingAutomatiskRetur -> "TOTRINNSVURDERING_RETUR" // TODO: Mangler å migrere typen i databasen
            is AutomatiskBehandlingStanset -> "STANS_AUTOMATISK_BEHANDLING" // TODO: Mangler å migrere typen i databasen
            is VedtaksperiodeReberegnet -> "VEDTAKSPERIODE_REBEREGNET" // TODO: Mangler å migrere typen i databasen
            is EndrePåVent -> "ENDRE_PA_VENT" // TODO: Mangler å migrere typen i databsen??
            is AutomatiskBehandlingStansetAvSaksbehandler -> "STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER" // TODO: Mangler å migrere typen i databsen??
            is OpphevStansAvSaksbehandler -> "OPPHEV_STANS_AUTOMATISK_BEHANDLING_SAKSBEHANDLER" // TODO: Mangler å migrere typen i databsen??
        }
}
