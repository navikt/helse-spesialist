package no.nav.helse.db

import no.nav.helse.db.api.PgAbonnementApiDao
import no.nav.helse.db.api.PgArbeidsgiverApiDao
import no.nav.helse.db.api.PgEgenAnsattApiDao
import no.nav.helse.db.api.PgGenerasjonApiRepository
import no.nav.helse.db.api.PgNotatApiDao
import no.nav.helse.db.api.PgOppgaveApiDao
import no.nav.helse.db.api.PgOverstyringApiDao
import no.nav.helse.db.api.PgPeriodehistorikkApiDao
import no.nav.helse.db.api.PgPersonApiDao
import no.nav.helse.db.api.PgPåVentApiDao
import no.nav.helse.db.api.PgRisikovurderingApiDao
import no.nav.helse.db.api.PgSnapshotApiDao
import no.nav.helse.db.api.PgTildelingApiDao
import no.nav.helse.db.api.PgTotrinnsvurderingApiDao
import no.nav.helse.db.api.PgVarselApiRepository
import no.nav.helse.db.api.PgVergemålApiDao
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import javax.sql.DataSource

class DBRepositories(dataSource: DataSource, tilgangskontroll: Tilgangskontroll) : Repositories {
    override val annulleringRepository = PgAnnulleringRepository(dataSource)
    override val avviksvurderingDao = PgAvviksvurderingDao(dataSource)
    override val behandlingsstatistikkDao = PgBehandlingsstatistikkDao(dataSource)
    override val commandContextDao = PgCommandContextDao(dataSource)
    override val definisjonDao = PgDefinisjonDao(dataSource)
    override val dialogDao = PgDialogDao(dataSource)
    override val dokumentDao = PgDokumentDao(dataSource)
    override val egenAnsattDao = PgEgenAnsattDao(dataSource)
    override val generasjonDao = PgGenerasjonDao(dataSource)
    override val meldingDao = PgMeldingDao(dataSource)
    override val meldingDuplikatkontrollDao = PgMeldingDuplikatkontrollDao(dataSource)
    override val notatDao = PgNotatDao(dataSource)
    override val oppgaveDao = PgOppgaveDao(dataSource)
    override val opptegnelseDao = PgOpptegnelseDao(dataSource)
    override val overstyringDao = PgOverstyringDao(dataSource)
    override val periodehistorikkDao = PgPeriodehistorikkDao(dataSource)
    override val personDao = PgPersonDao(dataSource)
    override val poisonPillDao = PgPoisonPillDao(dataSource)
    override val påVentDao = PgPåVentDao(dataSource)
    override val reservasjonDao = PgReservasjonDao(dataSource)
    override val saksbehandlerDao = PgSaksbehandlerDao(dataSource, tilgangskontroll)
    override val stansAutomatiskBehandlingDao = PgStansAutomatiskBehandlingDao(dataSource)
    override val tildelingDao = PgTildelingDao(dataSource)
    override val totrinnsvurderingDao = PgTotrinnsvurderingDao(dataSource)
    override val varselDao = PgVarselDao(dataSource)
    override val vedtakDao = PgVedtakDao(dataSource)
    override val vedtakBegrunnelseDao = PgVedtakBegrunnelseDao(dataSource)

    override val totrinnsvurderingRepository: TotrinnsvurderingRepository =
        PgTotrinnsvurderingRepository(overstyringDao, saksbehandlerDao, totrinnsvurderingDao)

    override val abonnementApiDao = PgAbonnementApiDao(dataSource)
    override val arbeidsgiverApiDao = PgArbeidsgiverApiDao(dataSource)
    override val egenAnsattApiDao = PgEgenAnsattApiDao(dataSource)
    override val generasjonApiRepository = PgGenerasjonApiRepository(dataSource)
    override val notatApiDao = PgNotatApiDao(dataSource)
    override val oppgaveApiDao = PgOppgaveApiDao(dataSource)
    override val overstyringApiDao = PgOverstyringApiDao(dataSource)
    override val periodehistorikkApiDao = PgPeriodehistorikkApiDao(dataSource)
    override val personApiDao = PgPersonApiDao(dataSource)
    override val påVentApiDao = PgPåVentApiDao(dataSource)
    override val risikovurderingApiDao = PgRisikovurderingApiDao(dataSource)
    override val snapshotApiDao = PgSnapshotApiDao(dataSource)
    override val tildelingApiDao = PgTildelingApiDao(dataSource)
    override val totrinnsvurderingApiDao = PgTotrinnsvurderingApiDao(dataSource)
    override val varselApiRepository = PgVarselApiRepository(dataSource)
    override val vergemålApiDao = PgVergemålApiDao(dataSource)
}
