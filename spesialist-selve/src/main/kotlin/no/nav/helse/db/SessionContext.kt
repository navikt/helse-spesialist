package no.nav.helse.db

interface SessionContext {
    val sykefraværstilfelleDao: SykefraværstilfelleDao
    val arbeidsgiverDao: ArbeidsgiverDao
    val avviksvurderingDao: AvviksvurderingDao
    val inntektskilderRepository: InntektskilderRepository
    val opptegnelseRepository: OpptegnelseRepository
    val dialogDao: DialogDao
    val notatDao: NotatDao
    val oppgaveDao: OppgaveDao
    val periodehistorikkDao: PeriodehistorikkDao
    val totrinnsvurderingDao: TotrinnsvurderingDao
    val vedtakDao: VedtakDao
    val saksbehandlerDao: SaksbehandlerDao
    val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao
    val tildelingDao: TildelingDao
    val generasjonDao: GenerasjonDao
    val reservasjonDao: ReservasjonDao
}
