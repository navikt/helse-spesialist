package no.nav.helse.db

interface SessionContext {
    val arbeidsforholdDao: ArbeidsforholdDao
    val arbeidsgiverDao: ArbeidsgiverDao
    val automatiseringDao: AutomatiseringDao
    val avviksvurderingDao: AvviksvurderingDao
    val commandContextDao: CommandContextDao
    val dialogDao: DialogDao
    val egenAnsattDao: EgenAnsattDao
    val generasjonDao: GenerasjonDao
    val inntektskilderRepository: InntektskilderRepository
    val meldingDao: MeldingDao
    val metrikkDao: MetrikkDao
    val notatDao: NotatDao
    val oppgaveDao: OppgaveDao
    val opptegnelseRepository: OpptegnelseRepository
    val overstyringDao: OverstyringDao
    val periodehistorikkDao: PeriodehistorikkDao
    val personDao: PersonDao
    val påVentDao: PåVentDao
    val reservasjonDao: ReservasjonDao
    val risikovurderingDao: RisikovurderingDao
    val saksbehandlerDao: SaksbehandlerDao
    val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
    val sykefraværstilfelleDao: SykefraværstilfelleDao
    val tildelingDao: TildelingDao
    val totrinnsvurderingDao: TotrinnsvurderingDao
    val utbetalingDao: UtbetalingDao
    val vedtakDao: VedtakDao
    val vergemålDao: VergemålDao
    val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao
}
