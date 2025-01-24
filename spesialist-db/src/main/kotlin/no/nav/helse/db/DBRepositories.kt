package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.api.PgAbonnementDao
import no.nav.helse.db.api.PgApiGenerasjonRepository
import no.nav.helse.db.api.PgEgenAnsattApiDao
import no.nav.helse.db.api.PgPåVentApiDao
import no.nav.helse.db.api.PgTildelingApiDao
import no.nav.helse.db.api.PgTotrinnsvurderingApiDao
import no.nav.helse.db.api.PgVergemålApiDao
import javax.sql.DataSource

class DBRepositories(dataSource: DataSource) : Repositories {
    override val abonnementDao = PgAbonnementDao(dataSource)
    override val annulleringRepository = PgAnnulleringRepository(dataSource)
    override val behandlingsstatistikkDao = PgBehandlingsstatistikkDao(dataSource)
    override val opptegnelseRepository = PgOpptegnelseRepository(dataSource)
    override val avviksvurderingDao = PgAvviksvurderingDao(dataSource)
    override val dialogDao = PgDialogDao(dataSource)
    override val notatDao = PgNotatDao(dataSource)
    override val oppgaveDao = PgOppgaveDao(dataSource)
    override val periodehistorikkDao = PgPeriodehistorikkDao(dataSource)
    override val totrinnsvurderingDao = PgTotrinnsvurderingDao(dataSource)
    override val vedtakDao = PgVedtakDao(dataSource)
    override val poisonPillDao = PgPoisonPillDao(dataSource)
    override val saksbehandlerDao = PgSaksbehandlerDao(dataSource)
    override val stansAutomatiskBehandlingDao = PgStansAutomatiskBehandlingDao(dataSource)
    override val tildelingDao = PgTildelingDao(dataSource)
    override val vedtakBegrunnelseDao = PgVedtakBegrunnelseDao(dataSource)
    override val generasjonDao = PgGenerasjonDao(dataSource)
    override val reservasjonDao = PgReservasjonDao(dataSource)
    override val commandContextDao = PgCommandContextDao(dataSource)
    override val dokumentDao = PgDokumentDao(dataSource)
    override val egenAnsattDao = PgEgenAnsattDao(dataSource)
    override val overstyringDao = PgOverstyringDao(dataSource)
    override val personDao = PgPersonDao(dataSource)
    override val påVentDao = PgPåVentDao(dataSource)
    override val definisjonDao = PgDefinisjonDao(dataSource)
    override val varselDao = PgVarselDao(dataSource)
    override val meldingDao = PgMeldingDao(dataSource)
    override val meldingDuplikatkontrollDao = PgMeldingDuplikatkontrollDao(dataSource)
    override val påVentApiDao = PgPåVentApiDao(dataSource)
    override val apiGenerasjonRepository = PgApiGenerasjonRepository(dataSource)
    override val egenAnsattApiDao = PgEgenAnsattApiDao(dataSource)
    override val vergemålApiDao = PgVergemålApiDao(dataSource)
    override val totrinnsvurderingApiDao = PgTotrinnsvurderingApiDao(dataSource)
    override val tildelingApiDao = PgTildelingApiDao(dataSource)

    override fun withSessionContext(session: Session) = DBSessionContext(session)
}
