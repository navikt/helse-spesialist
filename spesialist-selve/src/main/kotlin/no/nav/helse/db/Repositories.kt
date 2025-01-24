package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.api.AbonnementApiDao
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.GenerasjonApiDao
import no.nav.helse.db.api.GenerasjonApiRepository
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.db.api.SnapshotApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import no.nav.helse.db.api.VarselApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VergemålApiDao

interface Repositories {
    val abonnementApiDao: AbonnementApiDao
    val annulleringRepository: AnnulleringRepository
    val behandlingsstatistikkDao: BehandlingsstatistikkDao
    val opptegnelseRepository: OpptegnelseRepository
    val avviksvurderingDao: AvviksvurderingDao
    val dialogDao: DialogDao
    val notatDao: NotatDao
    val oppgaveDao: OppgaveDao
    val periodehistorikkDao: PeriodehistorikkDao
    val totrinnsvurderingDao: TotrinnsvurderingDao
    val vedtakDao: VedtakDao
    val poisonPillDao: PoisonPillDao
    val saksbehandlerDao: SaksbehandlerDao
    val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
    val tildelingDao: TildelingDao
    val vedtakBegrunnelseDao: VedtakBegrunnelseDao
    val generasjonDao: GenerasjonDao
    val reservasjonDao: ReservasjonDao
    val commandContextDao: CommandContextDao
    val dokumentDao: DokumentDao
    val egenAnsattDao: EgenAnsattDao
    val overstyringDao: OverstyringDao
    val personDao: PersonDao
    val påVentDao: PåVentDao
    val definisjonDao: DefinisjonDao
    val varselDao: VarselDao
    val meldingDao: MeldingDao
    val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao
    val påVentApiDao: PåVentApiDao
    val generasjonApiRepository: GenerasjonApiRepository
    val egenAnsattApiDao: EgenAnsattApiDao
    val vergemålApiDao: VergemålApiDao
    val totrinnsvurderingApiDao: TotrinnsvurderingApiDao
    val tildelingApiDao: TildelingApiDao
    val varselApiDao: VarselApiDao
    val varselApiRepository: VarselApiRepository
    val arbeidsgiverApiDao: ArbeidsgiverApiDao
    val generasjonApiDao: GenerasjonApiDao
    val notatApiDao: NotatApiDao
    val oppgaveApiDao: OppgaveApiDao
    val overstyringApiDao: OverstyringApiDao
    val periodehistorikkApiDao: PeriodehistorikkApiDao
    val personApiDao: PersonApiDao
    val risikovurderingApiDao: RisikovurderingApiDao
    val snapshotApiDao: SnapshotApiDao

    fun withSessionContext(session: Session): SessionContext
}
