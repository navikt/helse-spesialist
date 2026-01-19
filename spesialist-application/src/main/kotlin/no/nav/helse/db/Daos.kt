package no.nav.helse.db

import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.BehandlingApiRepository
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.spesialist.application.SaksbehandlerRepository

interface Daos {
    val annulleringRepository: AnnulleringRepository
    val behandlingsstatistikkDao: BehandlingsstatistikkDao
    val commandContextDao: CommandContextDao
    val definisjonDao: DefinisjonDao
    val dialogDao: DialogDao
    val dokumentDao: DokumentDao
    val egenAnsattDao: EgenAnsattDao
    val legacyBehandlingDao: LegacyBehandlingDao
    val meldingDao: MeldingDao
    val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao
    val notatDao: NotatDao
    val oppgaveDao: OppgaveDao
    val periodehistorikkDao: PeriodehistorikkDao
    val personDao: PersonDao
    val poisonPillDao: PoisonPillDao
    val påVentDao: PåVentDao
    val reservasjonDao: ReservasjonDao
    val saksbehandlerDao: SaksbehandlerDao
    val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
    val tildelingDao: TildelingDao
    val legacyVarselDao: LegacyVarselDao
    val vedtakDao: VedtakDao
    val vedtakBegrunnelseDao: VedtakBegrunnelseDao
    val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao

    val arbeidsgiverApiDao: ArbeidsgiverApiDao
    val egenAnsattApiDao: EgenAnsattApiDao
    val behandlingApiRepository: BehandlingApiRepository
    val notatApiDao: NotatApiDao
    val oppgaveApiDao: OppgaveApiDao
    val overstyringApiDao: OverstyringApiDao
    val periodehistorikkApiDao: PeriodehistorikkApiDao
    val personApiDao: PersonApiDao
    val påVentApiDao: PåVentApiDao
    val risikovurderingApiDao: RisikovurderingApiDao
    val tildelingApiDao: TildelingApiDao
    val varselApiRepository: VarselApiRepository
    val vergemålApiDao: VergemålApiDao
    val oppgaveRepository: OppgaveRepository
    val saksbehandlerRepository: SaksbehandlerRepository
}
