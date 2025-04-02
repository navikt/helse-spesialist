package no.nav.helse.db

import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.person.PersonRepository
import no.nav.helse.spesialist.application.DialogRepository
import no.nav.helse.spesialist.application.NotatRepository
import no.nav.helse.spesialist.application.OverstyringRepository
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.application.TilkommenInntektRepository
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository

interface SessionContext {
    val arbeidsforholdDao: ArbeidsforholdDao
    val arbeidsgiverDao: ArbeidsgiverDao
    val automatiseringDao: AutomatiseringDao
    val commandContextDao: CommandContextDao
    val dialogDao: DialogDao
    val egenAnsattDao: EgenAnsattDao
    val generasjonDao: GenerasjonDao
    val inntektskilderRepository: InntektskilderRepository
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
    val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao
    val vedtaksperiodeRepository: VedtaksperiodeRepository
    val personRepository: PersonRepository

    val totrinnsvurderingRepository: TotrinnsvurderingRepository
    val overstyringRepository: OverstyringRepository
    val notatRepository: NotatRepository
    val dialogRepository: DialogRepository
    val saksbehandlerRepository: SaksbehandlerRepository
    val avviksvurderingRepository: AvviksvurderingRepository
    val oppgaveRepository: OppgaveRepository
    val behandlingRepository: BehandlingRepository
    val tilkommenInntektRepository: TilkommenInntektRepository
}
