package no.nav.helse.db

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
    val annulleringRepository: AnnulleringRepository
    val avviksvurderingDao: AvviksvurderingDao
    val behandlingsstatistikkDao: BehandlingsstatistikkDao
    val commandContextDao: CommandContextDao
    val definisjonDao: DefinisjonDao
    val dialogDao: DialogDao
    val dokumentDao: DokumentDao
    val egenAnsattDao: EgenAnsattDao
    val generasjonDao: GenerasjonDao
    val meldingDao: MeldingDao
    val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao
    val notatDao: NotatDao
    val oppgaveDao: OppgaveDao
    val opptegnelseDao: OpptegnelseDao
    val overstyringDao: OverstyringDao
    val periodehistorikkDao: PeriodehistorikkDao
    val personDao: PersonDao
    val poisonPillDao: PoisonPillDao
    val påVentDao: PåVentDao
    val reservasjonDao: ReservasjonDao
    val saksbehandlerDao: SaksbehandlerDao
    val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
    val tildelingDao: TildelingDao
    val totrinnsvurderingDao: TotrinnsvurderingDao
    val varselDao: VarselDao
    val vedtakDao: VedtakDao
    val vedtakBegrunnelseDao: VedtakBegrunnelseDao

    val abonnementApiDao: AbonnementApiDao
    val arbeidsgiverApiDao: ArbeidsgiverApiDao
    val egenAnsattApiDao: EgenAnsattApiDao
    val generasjonApiDao: GenerasjonApiDao
    val generasjonApiRepository: GenerasjonApiRepository
    val notatApiDao: NotatApiDao
    val oppgaveApiDao: OppgaveApiDao
    val overstyringApiDao: OverstyringApiDao
    val periodehistorikkApiDao: PeriodehistorikkApiDao
    val personApiDao: PersonApiDao
    val påVentApiDao: PåVentApiDao
    val risikovurderingApiDao: RisikovurderingApiDao
    val snapshotApiDao: SnapshotApiDao
    val tildelingApiDao: TildelingApiDao
    val totrinnsvurderingApiDao: TotrinnsvurderingApiDao
    val varselApiDao: VarselApiDao
    val varselApiRepository: VarselApiRepository
    val vergemålApiDao: VergemålApiDao
}
