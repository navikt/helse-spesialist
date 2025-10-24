package no.nav.helse.db

import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.person.LegacyPersonRepository
import no.nav.helse.spesialist.application.ArbeidsgiverRepository
import no.nav.helse.spesialist.application.DialogRepository
import no.nav.helse.spesialist.application.NotatRepository
import no.nav.helse.spesialist.application.OverstyringRepository
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.application.PåVentRepository
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.application.TilkommenInntektRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository

interface SessionContext {
    val arbeidsforholdDao: ArbeidsforholdDao
    val automatiseringDao: AutomatiseringDao
    val commandContextDao: CommandContextDao
    val dialogDao: DialogDao
    val egenAnsattDao: EgenAnsattDao
    val legacyBehandlingDao: LegacyBehandlingDao
    val meldingDao: MeldingDao
    val metrikkDao: MetrikkDao
    val notatDao: NotatDao
    val oppgaveDao: OppgaveDao
    val opptegnelseDao: OpptegnelseDao
    val periodehistorikkDao: PeriodehistorikkDao
    val personDao: PersonDao
    val påVentDao: PåVentDao
    val reservasjonDao: ReservasjonDao
    val risikovurderingDao: RisikovurderingDao
    val saksbehandlerDao: SaksbehandlerDao
    val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
    val sykefraværstilfelleDao: SykefraværstilfelleDao
    val tildelingDao: TildelingDao
    val utbetalingDao: UtbetalingDao
    val vedtakDao: VedtakDao
    val vergemålDao: VergemålDao
    val dokumentDao: DokumentDao
    val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao
    val vedtaksperiodeRepository: VedtaksperiodeRepository
    val legacyPersonRepository: LegacyPersonRepository
    val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao

    val totrinnsvurderingRepository: TotrinnsvurderingRepository
    val overstyringRepository: OverstyringRepository
    val notatRepository: NotatRepository
    val dialogRepository: DialogRepository
    val saksbehandlerRepository: SaksbehandlerRepository
    val avviksvurderingRepository: AvviksvurderingRepository
    val oppgaveRepository: OppgaveRepository
    val behandlingRepository: BehandlingRepository
    val tilkommenInntektRepository: TilkommenInntektRepository
    val arbeidsgiverRepository: ArbeidsgiverRepository
    val annulleringRepository: AnnulleringRepository
    val påVentRepository: PåVentRepository
    val personRepository: PersonRepository
}
